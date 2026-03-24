sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/m/MessageToast",
    "sap/m/MessageBox"
], function (Controller, MessageToast, MessageBox) {
    "use strict";

    return Controller.extend("com.example.project.frontend.frontend.controller.Register", {
        onRegister: async function () {
            var sFullName = this.byId("registerFullNameInput").getValue().trim();
            var sEmail = this.byId("registerEmailInput").getValue().trim();
            var sUsername = this.byId("registerUsernameInput").getValue().trim();
            var sPassword = this.byId("registerPasswordInput").getValue().trim();

            if (!sFullName || !sEmail || !sUsername || !sPassword) {
                MessageToast.show("Please fill in all fields");
                return;
            }

            var aNameParts = sFullName.split(/\s+/);

            if (aNameParts.length < 2) {
                MessageBox.error("Please enter first name and last name");
                return;
            }

            var sFirstName = aNameParts[0];
            var sLastName = aNameParts.slice(1).join(" ");

            try {
                const oResponse = await fetch("http://localhost:8080/api/auth/register", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        username: sUsername,
                        firstName: sFirstName,
                        lastName: sLastName,
                        email: sEmail,
                        password: sPassword
                    })
                });

                if (!oResponse.ok) {
                    const sErrorText = await oResponse.text();
                    MessageBox.error("Registration failed: " + sErrorText);
                    return;
                }

                MessageToast.show("Registration successful");

                this.byId("registerFullNameInput").setValue("");
                this.byId("registerEmailInput").setValue("");
                this.byId("registerUsernameInput").setValue("");
                this.byId("registerPasswordInput").setValue("");

                this.getOwnerComponent().getRouter().navTo("RouteLogin");
            } catch (oError) {
                MessageBox.error("Cannot connect to backend: " + oError.message);
            }
        },

        onGoToLogin: function () {
            this.getOwnerComponent().getRouter().navTo("RouteLogin");
        }
    });
});