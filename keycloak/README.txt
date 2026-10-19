Demo realm notes
- Demo users (client1, rm1, admin1) have two-factor switched off so demos are quick.
  Before any shared or production use: add "CONFIGURE_TOTP" back to each user's requiredActions
  and delete the "wealth-api-tests" client (it allows password sign-in for API tests).
