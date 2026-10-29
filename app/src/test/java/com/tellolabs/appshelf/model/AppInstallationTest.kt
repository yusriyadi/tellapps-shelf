package com.tellolabs.appshelf.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppInstallationTest {
    @Test fun newerCatalogVersionOffersUpdate() {
        assertEquals(InstallationStatus.UPDATE_AVAILABLE, compareInstalledVersion(6, 7))
    }

    @Test fun equalCodesAreInstalledRegardlessOfVersionName() {
        assertEquals(InstallationStatus.INSTALLED, compareInstalledVersion(7, 7))
    }

    @Test fun olderCatalogVersionDoesNotOfferDowngradeAsUpdate() {
        assertEquals(InstallationStatus.NEWER_INSTALLED, compareInstalledVersion(7, 6))
    }

    @Test fun comparesFullLongVersionCode() {
        val installed = Int.MAX_VALUE.toLong() + 1
        assertEquals(InstallationStatus.UPDATE_AVAILABLE, compareInstalledVersion(installed, installed + 1))
        assertEquals(InstallationStatus.NEWER_INSTALLED, compareInstalledVersion(installed + 1, installed))
    }

    @Test fun missingOrInvalidVersionCodeRemainsUnknown() {
        assertEquals(InstallationStatus.UNKNOWN, compareInstalledVersion(7, null))
        assertEquals(InstallationStatus.UNKNOWN, compareInstalledVersion(7, -1))
        assertEquals(InstallationStatus.UNKNOWN, compareInstalledVersion(-1, 7))
    }
}
