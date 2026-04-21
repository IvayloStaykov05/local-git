sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/MessageBox",
    "sap/m/MessageToast"
], function (Controller, JSONModel, MessageBox, MessageToast) {
    "use strict";

    return Controller.extend("com.example.project.frontend.frontend.controller.AdminDashboard", {

        onInit: function () {
            var sToken = localStorage.getItem("token");
            var oStoredUser = {};

            try {
                oStoredUser = JSON.parse(localStorage.getItem("user") || "{}");
            } catch (e) {
                oStoredUser = {};
            }

            if (!sToken) {
                this.getOwnerComponent().getRouter().navTo("RouteLogin");
                return;
            }

            if (oStoredUser.systemRole !== "ADMIN") {
                MessageBox.error("You do not have access to this page.");
                this.getOwnerComponent().getRouter().navTo("RouteDashboard");
                return;
            }

            var oModel = new JSONModel({
                currentUser: oStoredUser,
                search: "",
                documents: [],
                users: []
            });

            this.getView().setModel(oModel, "admin");
            this._loadAdminData("");
        },

        _getAuthHeaders: function () {
            return {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + localStorage.getItem("token")
            };
        },

        _handleUnauthorized: function () {
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            this.getOwnerComponent().getRouter().navTo("RouteLogin");
        },

        _loadAdminData: async function (sSearch) {
            var oModel = this.getView().getModel("admin");
            var sEncodedSearch = encodeURIComponent((sSearch || "").trim());

            try {
                const [oDocumentsResponse, oUsersResponse] = await Promise.all([
                    fetch("http://localhost:8080/api/admin/documents?search=" + sEncodedSearch, {
                        method: "GET",
                        headers: this._getAuthHeaders()
                    }),
                    fetch("http://localhost:8080/api/admin/users/search?search=" + sEncodedSearch, {
                        method: "GET",
                        headers: this._getAuthHeaders()
                    })
                ]);

                if (
                    oDocumentsResponse.status === 401 || oDocumentsResponse.status === 403 ||
                    oUsersResponse.status === 401 || oUsersResponse.status === 403
                ) {
                    this._handleUnauthorized();
                    return;
                }

                const sDocumentsText = await oDocumentsResponse.text();
                const sUsersText = await oUsersResponse.text();

                if (!oDocumentsResponse.ok) {
                    throw new Error(sDocumentsText || "Cannot load admin documents");
                }

                if (!oUsersResponse.ok) {
                    throw new Error(sUsersText || "Cannot load admin users");
                }

                oModel.setProperty("/documents", sDocumentsText ? JSON.parse(sDocumentsText) : []);
                oModel.setProperty("/users", sUsersText ? JSON.parse(sUsersText) : []);

            } catch (oError) {
                MessageBox.error("Admin data loading failed: " + oError.message);
            }
        },

        onSearchAdmin: function (oEvent) {
            var sValue = oEvent.getSource().getValue();
            this.getView().getModel("admin").setProperty("/search", sValue);
            this._loadAdminData(sValue);
        },

        onSearchAdminLiveChange: function (oEvent) {
            var sValue = oEvent.getParameter("value");
            this.getView().getModel("admin").setProperty("/search", sValue);
            this._loadAdminData(sValue);
        },

        onOpenDocument: function (oEvent) {
            var oContext = oEvent.getSource().getBindingContext("admin");
            if (!oContext) {
                return;
            }

            var oDocument = oContext.getObject();

            this.getOwnerComponent().getRouter().navTo("RouteDocumentDetails", {
                documentId: String(oDocument.id)
            });
        },

        onLogout: function () {
            localStorage.removeItem("token");
            localStorage.removeItem("user");
            MessageToast.show("Logged out successfully");
            this.getOwnerComponent().getRouter().navTo("RouteLogin");
        },

        formatDateTime: function (sValue) {
            if (!sValue) {
                return "";
            }

            var oDate = new Date(sValue);

            if (isNaN(oDate.getTime())) {
                return sValue;
            }

            return oDate.toLocaleString();
        }
    });
});