sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/m/MessageToast",
    "sap/m/MessageBox"
], function (Controller, MessageToast, MessageBox) {
    "use strict";

    return Controller.extend("com.example.project.frontend.frontend.controller.Login", {
        onLogin: async function () {
            var sUsernameOrEmail = this.byId("loginUsernameInput").getValue().trim();
            var sPassword = this.byId("loginPasswordInput").getValue().trim();

            if (!sUsernameOrEmail || !sPassword) {
                MessageToast.show("Please fill in username and password");
                return;
            }

            try {
                const oResponse = await fetch("http://localhost:8080/api/auth/login", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        usernameOrEmail: sUsernameOrEmail,
                        password: sPassword
                    })
                });

                if (!oResponse.ok) {
                    const sErrorText = await oResponse.text();
                    MessageBox.error("Login failed: " + sErrorText);
                    return;
                }

                const oData = await oResponse.json().catch(function () {
                    return null;
                });

                if (oData && oData.token) {
                    localStorage.setItem("token", oData.token);
                }

                MessageToast.show("Login successful");

                this.byId("loginUsernameInput").setValue("");
                this.byId("loginPasswordInput").setValue("");

                // ако имаш home route, смени RouteHome с правилния route
                // this.getOwnerComponent().getRouter().navTo("RouteHome");
            } catch (oError) {
                MessageBox.error("Cannot connect to backend: " + oError.message);
            }
        },

        onGoToRegister: function () {
            this.getOwnerComponent().getRouter().navTo("RouteRegister");
        },

        onForgotPassword: function () {
            MessageToast.show("Forgot password clicked");
        }
    });
});