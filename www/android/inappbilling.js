/*
 * Copyright (C) 2012-2013 by Guillaume Charhon
 */
var inappbilling = { 

    // Initialize the plugin
    init: function (success, fail) {
      return cordova.exec( success, fail,
                           "InAppBillingPlugin",
                           "init", ["null"]);
    },
	/**
	 * get already own items
	 * @param success
	 * @param fail
	 * @returns [productId,...]
	 */
    getPurchases: function (success, fail) {
        return cordova.exec( success, fail,
            "InAppBillingPlugin",
            "getPurchases", ["null"]);
    },
	/**
	 * get skudetails for list of products in skuList
	 * @param success
	 * @param fail
	 * @param skuList
	 * @returns [{productId, type, price, title, description},...]
	 */
    getSkuDetails: function (success, fail, skuList) {
        return cordova.exec( success, fail,
            "InAppBillingPlugin",
            "getSkuDetails", skuList);
    },
	/**
	 * purchase an item
	 * @param success
	 * @param fail
	 * @param productId
	 * @param payload
	 * @returns [INAPP_PURCHASE_DATA, INAPP_DATA_SIGNATURE]
	 */
    buy: function (success, fail, productId, payload) {
		if(typeof payload==='undefined')
			payload="";
        return cordova.exec( success, fail,
            "InAppBillingPlugin",
            "buy", [productId, payload]);
    },
	/**
	 * get info of old purchase
	 * @param success
	 * @param fail
	 * @param sku
	 * @returns [INAPP_PURCHASE_DATA, INAPP_DATA_SIGNATURE]
	 */
    getPurchaseReceipt: function (success, fail, sku) {
        return cordova.exec( success, fail,
            "InAppBillingPlugin",
            "getPurchaseReceipt", [sku]);
    },
    // subscribe to an item
    subscribe: function (success, fail, productId) {
        return cordova.exec( success, fail,
            "InAppBillingPlugin",
            "subscribe", [productId]);
    },
	/**
	 * consume a purchased item
	 * @param success
	 * @param fail
	 * @param productId
	 * @returns [INAPP_PURCHASE_DATA, INAPP_DATA_SIGNATURE]
	 */
    consumePurchase: function (success, fail, productId) {
        return cordova.exec( success, fail,
            "InAppBillingPlugin",
            "consumePurchase", [productId]);
    }
};

module.exports = inappbilling;