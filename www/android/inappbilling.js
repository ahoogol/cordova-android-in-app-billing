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
    // get already own items
    getPurchases: function (success, fail) {
        return cordova.exec( success, fail,
            "InAppBillingPlugin",
            "getPurchases", ["null"]);
    },
    // get skudetails for list of products in skuList
    getSkuDetails: function (success, fail, skuList) {
        return cordova.exec( success, fail,
            "InAppBillingPlugin",
            "getSkuDetails", skuList);
    },
    // purchase an item
    buy: function (success, fail, productId) {
        return cordova.exec( success, fail,
            "InAppBillingPlugin",
            "buy", [productId]);
    },
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
    // consume a purchased item
    consumePurchase: function (success, fail, productId) {
        return cordova.exec( success, fail,
            "InAppBillingPlugin",
            "consumePurchase", [productId]);
    }
};

module.exports = inappbilling;