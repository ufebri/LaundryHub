package com.raylabs.laundryhub.core.data.service

import android.accounts.Account

interface GoogleSheetsAccountProvider {
    fun getAuthorizedAccount(): Account?
    fun getSignedInEmail(): String?
}
