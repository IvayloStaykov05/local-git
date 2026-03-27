sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/core/Fragment",
    "sap/m/MessageToast",
    "sap/m/MessageBox"
], function (Controller, Fragment, MessageToast, MessageBox) {
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

                const sResponseText = await oResponse.text();

                if (!oResponse.ok) {
                    MessageBox.error("Login failed: " + (sResponseText || "Unknown error"));
                    return;
                }

                var oData = null;
                try {
                    oData = sResponseText ? JSON.parse(sResponseText) : null;
                } catch (e) {
                    oData = null;
                }

                if (oData && oData.token) {
                    localStorage.setItem("token", oData.token);
                }

                MessageToast.show("Login successful");

                this.byId("loginUsernameInput").setValue("");
                this.byId("loginPasswordInput").setValue("");

            } catch (oError) {
                MessageBox.error("Cannot connect to backend: " + oError.message);
            }
        },

        onGoToRegister: function () {
            this.getOwnerComponent().getRouter().navTo("RouteRegister");
        },

        onForgotPassword: async function () {
            try {
                if (!this._oForgotPasswordDialog) {
                    this._oForgotPasswordDialog = await Fragment.load({
                        id: this.getView().getId(),
                        name: "com.example.project.frontend.frontend.view.fragments.ForgotPasswordDialog",
                        controller: this
                    });

                    this.getView().addDependent(this._oForgotPasswordDialog);
                }

                this._oForgotPasswordDialog.open();
            } catch (oError) {
                MessageBox.error("Cannot open dialog: " + oError.message);
            }
        },

        onCloseForgotPasswordDialog: function () {
            if (this._oForgotPasswordDialog) {
                this._oForgotPasswordDialog.close();
            }
        },

        _getForgotPasswordField: function (sFieldId) {
            return Fragment.byId(this.getView().getId(), sFieldId);
        },

        _clearForgotPasswordForm: function () {
            this._getForgotPasswordField("forgotUsernameOrEmailInput").setValue("");
            this._getForgotPasswordField("forgotNewPasswordInput").setValue("");
            this._getForgotPasswordField("forgotConfirmPasswordInput").setValue("");
        },

        onSubmitForgotPassword: async function () {
            try {
                var oUsernameOrEmailInput = this._getForgotPasswordField("forgotUsernameOrEmailInput");
                var oNewPasswordInput = this._getForgotPasswordField("forgotNewPasswordInput");
                var oConfirmPasswordInput = this._getForgotPasswordField("forgotConfirmPasswordInput");

                if (!oUsernameOrEmailInput || !oNewPasswordInput || !oConfirmPasswordInput) {
                    MessageBox.error("Forgot password form controls not found");
                    return;
                }

                var sUsernameOrEmail = oUsernameOrEmailInput.getValue().trim();
                var sNewPassword = oNewPasswordInput.getValue().trim();
                var sConfirmPassword = oConfirmPasswordInput.getValue().trim();

                if (!sUsernameOrEmail || !sNewPassword || !sConfirmPassword) {
                    MessageBox.error("Please fill all fields");
                    return;
                }

                if (sNewPassword !== sConfirmPassword) {
                    MessageBox.error("Passwords do not match");
                    return;
                }

                const oResponse = await fetch("http://localhost:8080/api/auth/forgot-password", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        usernameOrEmail: sUsernameOrEmail,
                        newPassword: sNewPassword,
                        confirmPassword: sConfirmPassword
                    })
                });

                const sText = await oResponse.text();

                if (!oResponse.ok) {
                    MessageBox.error(sText || "Error while changing password");
                    return;
                }

                MessageToast.show(sText || "Password changed successfully");

                this._clearForgotPasswordForm();
                this.onCloseForgotPasswordDialog();

            } catch (oError) {
                MessageBox.error("Backend error: " + oError.message);
            }
        }

    });
});