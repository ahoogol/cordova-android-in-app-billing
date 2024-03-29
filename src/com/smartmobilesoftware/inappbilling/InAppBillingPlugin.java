package com.smartmobilesoftware.inappbilling;


import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.smartmobilesoftware.util.IabException;
import com.smartmobilesoftware.util.Purchase;
import com.smartmobilesoftware.util.IabHelper;
import com.smartmobilesoftware.util.IabResult;
import com.smartmobilesoftware.util.Inventory;

import android.content.Intent;
import android.util.Log;


/**
 * In App Billing Plugin
 * @author Guillaume Charhon - Smart Mobile Software
 *
 */
public class InAppBillingPlugin extends CordovaPlugin {
	private final Boolean ENABLE_DEBUG_LOGGING = true;
	private final String TAG = "CORDOVA_BILLING";
	
	
	/* base64EncodedPublicKey should be YOUR APPLICATION'S PUBLIC KEY
	 * (that you got from the Google Play developer console). This is not your
	 * developer public key, it's the *app-specific* public key.
	 *
	 * Instead of just storing the entire literal string here embedded in the
	 * program,  construct the key at runtime from pieces or
	 * use bit manipulation (for example, XOR with some other string) to hide
	 * the actual key.  The key itself is not secret information, but we don't
	 * want to make it easy for an attacker to replace the public key with one
	 * of their own and then fake messages from the server.
	 */
	
	// (arbitrary) request code for the purchase flow
	static final int RC_REQUEST = 10001;
	
	// The helper object
	IabHelper mHelper;
	
	// A quite up to date inventory of available items and purchase items
	Inventory myInventory; 
	
	CallbackContext callbackContext;
	
	@Override
	/**
	 * Called by each javascript plugin function
	 */
	public boolean execute(String action, JSONArray data, final CallbackContext callbackContext) {
		this.callbackContext = callbackContext;
		// Check if the action has a handler
		Boolean isValidAction = true;
		
		try {
			// Action selector
			if ("init".equals(action)) {
				// Initialize
				init();
			} else if ("getPurchases".equals(action)) {
				// Get the list of purchases
				JSONArray jsonSkuList = new JSONArray();
				jsonSkuList = getPurchases();
				
				// Call the javascript back
				callbackContext.success(jsonSkuList);
			} else if ("getSkuDetails".equals(action)) {
				// get sku details for list of skus
				JSONArray jsonSkuList = new JSONArray();
				List<String> skus = new ArrayList<String>();
				for (int i = 0; i < data.length(); i++) {
					skus.add(data.getString(i));
				}
				//skus.add("android.test.purchased");
				Log.d(TAG, "E: Querying inventory for skus " + skus);
				myInventory = mHelper.queryInventory(true, skus);
				Log.d(TAG, "E: Got results for skus");
				for (int i = 0; i < skus.size(); i++) {
					Log.d(TAG, "Getting details for " + skus.get(i));
					if (myInventory.hasDetails(skus.get(i))) {
						JSONObject details = myInventory.getSkuDetails(skus.get(i)).toJson();
						Log.d(TAG, "Got details " + details);
						jsonSkuList.put(details);
					} else {
						Log.d(TAG, "Don't have details...");
					}
					
				}
				callbackContext.success(jsonSkuList);
			} else if ("buy".equals(action)) {
				// Buy an item
				
				// Get Product Id 
				final String sku = data.getString(0);
				final String payload = data.getString(1);
				buy(sku, payload);
	
			} else if ("getPurchaseReceipt".equals(action)){
				final String sku = data.getString(0);
				
				Log.d(TAG, "Getting purchase info for " + sku);
				
				Purchase purchase = myInventory.getPurchase(sku);
				JSONObject receipt = new JSONObject(purchase.getOriginalJson());
				
				Log.d(TAG, "Purchase receipt: " + receipt.toString());
				
				JSONArray purchaseDetails=new JSONArray();
				purchaseDetails.put(purchase.getOriginalJson());
				purchaseDetails.put(purchase.getSignature());
				callbackContext.success(purchaseDetails);
				
			} else if ("subscribe".equals(action)) {
				// Subscribe to an item
				
				// Get Product Id 
				final String sku = data.getString(0);
				subscribe(sku);
	
			} else if ("consumePurchase".equals(action)) {
				consumePurchase(data);
				
			} else {
				// No handler for the action
				isValidAction = false;
			}
		
		} catch (IllegalStateException e){
			callbackContext.error(e.getMessage());
		} catch (JSONException e){
			callbackContext.error(e.getMessage());
		} catch (IabException e) {
			callbackContext.error(e.getMessage());
		}
		
		// Method not found
		return isValidAction;
	}
	
	// Initialize the plugin
	private void init(){
	
		int API_KEY_ID = cordova.getActivity().getResources().getIdentifier("iab_api_key", "string", cordova.getActivity().getPackageName());
        String API_KEY = cordova.getActivity().getResources().getString(API_KEY_ID);
		String base64EncodedPublicKey=API_KEY;
		
		// Some sanity checks to see if the developer (that's you!) really followed the
		// instructions to run this plugin
		if (base64EncodedPublicKey.contains("CONSTRUCT_YOUR")) 
			throw new RuntimeException("Please put your app's public key in InAppBillingPlugin.java. See ReadMe.");
		
		// Create the helper, passing it our context and the public key to verify signatures with
		Log.d(TAG, "Creating IAB helper.");
		mHelper = new IabHelper(cordova.getActivity().getApplicationContext(), base64EncodedPublicKey);
		
		// enable debug logging (for a production application, you should set this to false).
		mHelper.enableDebugLogging(ENABLE_DEBUG_LOGGING);
		
		// Start setup. This is asynchronous and the specified listener
		// will be called once setup completes.
		Log.d(TAG, "Starting setup.");
		
		mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
			public void onIabSetupFinished(IabResult result) {
				Log.d(TAG, "Setup finished.");

				if (!result.isSuccess()) {
					// Oh no, there was a problem.
					callbackContext.error("Problem setting up in-app billing: " + result);
					return;
				}
				
				// Have we been disposed of in the meantime? If so, quit.
				if (mHelper == null) {
					callbackContext.error("The billing helper has been disposed");
				}

				// Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
				Log.d(TAG, "Setup successful. Querying inventory.");
				mHelper.queryInventoryAsync(mGotInventoryListener);
			}

			
		});
	}
	
	// Buy an item
	private void buy(final String sku, final String payload){
		/* TODO: for security, generate your payload here for verification. See the comments on 
		 *        verifyDeveloperPayload() for more info. Since this is a sample, we just use 
		 *        an empty string, but on a production app you should generate this. */
		//final String payload = "";
		
		if (mHelper == null){
			callbackContext.error("Billing plugin was not initialized");
			return;
		}
		
		this.cordova.setActivityResultCallback(this);
		
		mHelper.launchPurchaseFlow(cordova.getActivity(), sku, RC_REQUEST, 
				mPurchaseFinishedListener, payload);

	}
	
	// Buy an item
	private void subscribe(final String sku){
		if (mHelper == null){
			callbackContext.error("Billing plugin was not initialized");
			return;
		}
		if (!mHelper.subscriptionsSupported()) {
			callbackContext.error("Subscriptions not supported on your device yet. Sorry!");
			return;
		}
		
		/* TODO: for security, generate your payload here for verification. See the comments on 
		 *        verifyDeveloperPayload() for more info. Since this is a sample, we just use 
		 *        an empty string, but on a production app you should generate this. */
		final String payload = "";
		
		
		
		this.cordova.setActivityResultCallback(this);
		Log.d(TAG, "Launching purchase flow for subscription.");

		mHelper.launchPurchaseFlow(cordova.getActivity(), sku, IabHelper.ITEM_TYPE_SUBS, RC_REQUEST, mPurchaseFinishedListener, payload);   
	}
	

	// Get the list of purchases
	private JSONArray getPurchases(){
		// Get the list of owned items
		if(myInventory == null){
			callbackContext.error("Billing plugin was not initialized");
			return new JSONArray();
		}
		List<String>skuList = myInventory.getAllOwnedSkus();
		
		// Convert the java list to json
		JSONArray jsonSkuList = new JSONArray();
		for (String sku : skuList) {
			jsonSkuList.put(sku);
		}
		
		return jsonSkuList;
		
	}
	

	// Consume a purchase
	private void consumePurchase(JSONArray data) throws JSONException{
		
		if (mHelper == null){
			callbackContext.error("Did you forget to initialize the plugin?");
			return;
		} 
		
		String sku = data.getString(0);
		
		// Get the purchase from the inventory
		Purchase purchase = myInventory.getPurchase(sku);
		if (purchase != null)
			// Consume it
			mHelper.consumeAsync(purchase, mConsumeFinishedListener);
		else
			callbackContext.error(sku + " is not owned so it cannot be consumed");
	}
	
	// Listener that's called when we finish querying the items and subscriptions we own
	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
			Log.d(TAG, "Inside mGotInventoryListener");
			if (!hasErrorsAndUpdateInventory(result, inventory)){
				
			}

			Log.d(TAG, "Query inventory was successful.");
			callbackContext.success();
			
		}
	};
	
	// Check if there is any errors in the iabResult and update the inventory
	private Boolean hasErrorsAndUpdateInventory(IabResult result, Inventory inventory){
		if (result.isFailure()) {
			callbackContext.error("Failed to query inventory: " + result);
			return true;
		}
		
		// Have we been disposed of in the meantime? If so, quit.
		if (mHelper == null) {
			callbackContext.error("The billing helper has been disposed");
			return true;
		}
		
		// Update the inventory
		myInventory = inventory;
		
		return false;
	}
	
	// Callback for when a purchase is finished
	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
			
			// Have we been disposed of in the meantime? If so, quit.
			if (mHelper == null) {
				callbackContext.error("The billing helper has been disposed");
			}
			
			if (result.isFailure()) {
				JSONObject response = new JSONObject();
				try {
					response.put("code", result.getResponse());
					response.put("message", result.getMessage());
					callbackContext.error(response);
				} catch (JSONException e) {
					callbackContext.error("Fatal: JSONException");
				}

				return;
			}
			
			if (!verifyDeveloperPayload(purchase)) {
				callbackContext.error("Error purchasing. Authenticity verification failed.");
				return;
			}

			Log.d(TAG, "Purchase successful.");
			
			// add the purchase to the inventory
			myInventory.addPurchase(purchase);
			JSONArray purchaseDetails = new JSONArray();
			purchaseDetails.put(purchase.getOriginalJson());
			purchaseDetails.put(purchase.getSignature());
			callbackContext.success(purchaseDetails);

		}
	};
	
	// Called when consumption is complete
	IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
		public void onConsumeFinished(Purchase purchase, IabResult result) {
			Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

			// We know this is the "gas" sku because it's the only one we consume,
			// so we don't check which sku was consumed. If you have more than one
			// sku, you probably should check...
			if (result.isSuccess()) {
				// successfully consumed, so we apply the effects of the item in our
				// game world's logic
				
				// remove the item from the inventory
				myInventory.erasePurchase(purchase.getSku());
				Log.d(TAG, "Consumption successful. .");
				
				JSONArray purchaseDetails = new JSONArray();
				purchaseDetails.put(purchase.getOriginalJson());
				purchaseDetails.put(purchase.getSignature());
				callbackContext.success(purchaseDetails);
				
			}
			else {
				callbackContext.error("Error while consuming: " + result);
			}
			
		}
	};
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);

		// Pass on the activity result to the helper for handling
		if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			super.onActivityResult(requestCode, resultCode, data);
		}
		else {
			Log.d(TAG, "onActivityResult handled by IABUtil.");
		}
	}
	
	/** Verifies the developer payload of a purchase. */
	boolean verifyDeveloperPayload(Purchase p) {
		@SuppressWarnings("unused")
		String payload = p.getDeveloperPayload();
		
		/*
		 * TODO: verify that the developer payload of the purchase is correct. It will be
		 * the same one that you sent when initiating the purchase.
		 * 
		 * WARNING: Locally generating a random string when starting a purchase and 
		 * verifying it here might seem like a good approach, but this will fail in the 
		 * case where the user purchases an item on one device and then uses your app on 
		 * a different device, because on the other device you will not have access to the
		 * random string you originally generated.
		 *
		 * So a good developer payload has these characteristics:
		 * 
		 * 1. If two different users purchase an item, the payload is different between them,
		 *    so that one user's purchase can't be replayed to another user.
		 * 
		 * 2. The payload must be such that you can verify it even when the app wasn't the
		 *    one who initiated the purchase flow (so that items purchased by the user on 
		 *    one device work on other devices owned by the user).
		 * 
		 * Using your own server to store and verify developer payloads across app
		 * installations is recommended.
		 */
		
		return true;
	}
	
	// We're being destroyed. It's important to dispose of the helper here!
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// very important:
		Log.d(TAG, "Destroying helper.");
		if (mHelper != null) {
			mHelper.dispose();
			mHelper = null;
		}
	}
	
}
