sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/MessageToast",
    "sap/m/MessageBox"
], function (Controller, JSONModel, MessageToast, MessageBox) {
    "use strict";

    return Controller.extend("com.example.project.frontend.frontend.controller.Dashboard", {

        onInit: function () {
            var sToken = localStorage.getItem("token");

            if (!sToken) {
                this.getOwnerComponent().getRouter().navTo("RouteLogin");
                return;
            }

            var oDashboardModel = new JSONModel({
                myInfo: {
                    username: "",
                    email: "",
                    systemRole: ""
                },
                myDocuments: [],
                peopleWithWork: [],
                notifications: []
            });

            this.getView().setModel(oDashboardModel, "dashboard");
            this._loadDashboard();
        },

        _loadDashboard: async function () {
            var sToken = localStorage.getItem("token");
            var oStoredUser;
            var sStoredUsername = "";

            try {
                oStoredUser = JSON.parse(localStorage.getItem("user") || "{}");
                sStoredUsername = oStoredUser.username || oStoredUser.email || "";
            } catch (e) {
                sStoredUsername = "";
            }

            try {
                const oResponse = await fetch("http://localhost:8080/api/dashboard", {
                    method: "GET",
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": "Bearer " + sToken
                    }
                });

                const sText = await oResponse.text();

                if (!oResponse.ok) {
                    if (oResponse.status === 401 || oResponse.status === 403) {
                        localStorage.removeItem("token");
                        localStorage.removeItem("user");
                        this.getOwnerComponent().getRouter().navTo("RouteLogin");
                        return;
                    }

                    throw new Error(sText || "Dashboard loading failed");
                }

                var oData = sText ? JSON.parse(sText) : {};
                var oMyInfo = oData.myInfo || {};

                this.getView().getModel("dashboard").setData({
                    myInfo: {
                        username: oMyInfo.username || sStoredUsername || "User",
                        email: oMyInfo.email || oStoredUser.email || "",
                    },
                    myDocuments: Array.isArray(oData.myDocuments) ? oData.myDocuments : [],
                    peopleWithWork: Array.isArray(oData.peopleWithWork) ? oData.peopleWithWork : [],
                    notifications: Array.isArray(oData.notifications) ? oData.notifications : []
                });

            } catch (oError) {
                MessageBox.error("Cannot load dashboard: " + oError.message);
            }
        },

        onLogout: function () {
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            MessageToast.show("Logged out successfully");
            this.getOwnerComponent().getRouter().navTo("RouteLogin");
        },

        onCreateDocument: function () {
            MessageToast.show("Create document ще го вържем след това.");
        },

        onOpenDocument: function () {
            MessageToast.show("Open document ще го вържем след това.");
        },

        onOpenPersonWork: function () {
            MessageToast.show("Person work details ще ги вържем след това.");
        },

        onSearchDashboard: function () {
            MessageToast.show("Search ще го вържем след това.");
        }
    });
});