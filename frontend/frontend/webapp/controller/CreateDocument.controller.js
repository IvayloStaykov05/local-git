sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/m/MessageBox",
    "sap/m/MessageToast"
], function (Controller, JSONModel, MessageBox, MessageToast) {
    "use strict";

    return Controller.extend("com.example.project.frontend.frontend.controller.CreateDocument", {
        onInit: function () {
            var sToken = localStorage.getItem("token");
            if (!sToken) {
                this.getOwnerComponent().getRouter().navTo("RouteLogin");
                return;
            }

            this.getView().setModel(new JSONModel({
                title: "",
                description: "",
                fileName: ""
            }), "createDocument");
        },

        onBackToDashboard: function () {
            this.getOwnerComponent().getRouter().navTo("RouteDashboard");
        },

        onFileChange: function (oEvent) {
            var oFile = oEvent.getParameter("files") && oEvent.getParameter("files")[0];
            this.getView().getModel("createDocument").setProperty("/fileName", oFile ? oFile.name : "");
        },

        onCreateDocument: async function () {
            var oModel = this.getView().getModel("createDocument");
            var sTitle = (oModel.getProperty("/title") || "").trim();
            var sDescription = (oModel.getProperty("/description") || "").trim();
            var oUploader = this.byId("createDocumentFileUploader");
            var oFile = oUploader && oUploader.oFileUpload && oUploader.oFileUpload.files && oUploader.oFileUpload.files[0];
            var sToken = localStorage.getItem("token");

            if (!sTitle) {
                MessageBox.warning("Моля въведи име на документа.");
                return;
            }

            if (!oFile) {
                MessageBox.warning("Моля избери файл.");
                return;
            }

            try {
                var oFormData = new FormData();
                oFormData.append("title", sTitle);
                oFormData.append("description", sDescription);
                oFormData.append("file", oFile);

                var oResponse = await fetch("http://localhost:8080/api/documents/createFirst", {
                    method: "POST",
                    headers: {
                        "Authorization": "Bearer " + sToken
                    },
                    body: oFormData
                });

                var sText = await oResponse.text();

                if (!oResponse.ok) {
                    throw new Error(sText || "Document creation failed");
                }

                var oCreatedDocument = sText ? JSON.parse(sText) : {};
                MessageToast.show("Документът е създаден успешно.");

                this.getOwnerComponent().getRouter().navTo("RouteDocumentDetails", {
                    documentId: String(oCreatedDocument.id)
                });
            } catch (oError) {
                MessageBox.error("Неуспешно създаване на документ: " + oError.message);
            }
        }
    });
});