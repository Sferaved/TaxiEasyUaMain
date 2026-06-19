package com.taxi.easy.ua.utils.download;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AppUpdatePromptHelperTest {

    @Test
    public void shouldShowUpdateDialog_nullInfo_returnsFalse() {
        assertFalse(AppUpdatePromptHelper.shouldShowUpdateDialog(null));
    }

    @Test
    public void shouldShowUpdateDialog_updateNotAvailable_returnsFalse() {
        AppUpdateInfo info = mock(AppUpdateInfo.class);
        when(info.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_NOT_AVAILABLE);

        assertFalse(AppUpdatePromptHelper.shouldShowUpdateDialog(info));
    }

    @Test
    public void shouldShowUpdateDialog_immediateNotAllowed_returnsFalse() {
        AppUpdateInfo info = mock(AppUpdateInfo.class);
        when(info.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_AVAILABLE);
        when(info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)).thenReturn(false);

        assertFalse(AppUpdatePromptHelper.shouldShowUpdateDialog(info));
    }

    @Test
    public void shouldShowUpdateDialog_downloadInProgress_returnsFalse() {
        AppUpdateInfo info = mock(AppUpdateInfo.class);
        when(info.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_AVAILABLE);
        when(info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)).thenReturn(true);
        when(info.installStatus()).thenReturn(InstallStatus.DOWNLOADING);

        assertFalse(AppUpdatePromptHelper.shouldShowUpdateDialog(info));
    }

    @Test
    public void shouldShowUpdateDialog_installInProgress_returnsFalse() {
        AppUpdateInfo info = mock(AppUpdateInfo.class);
        when(info.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_AVAILABLE);
        when(info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)).thenReturn(true);
        when(info.installStatus()).thenReturn(InstallStatus.INSTALLING);

        assertFalse(AppUpdatePromptHelper.shouldShowUpdateDialog(info));
    }

    @Test
    public void shouldShowUpdateDialog_readyToStart_returnsTrue() {
        AppUpdateInfo info = mock(AppUpdateInfo.class);
        when(info.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_AVAILABLE);
        when(info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)).thenReturn(true);
        when(info.installStatus()).thenReturn(InstallStatus.PENDING);

        assertTrue(AppUpdatePromptHelper.shouldShowUpdateDialog(info));
    }

    @Test
    public void shouldShowUpdateDialog_downloadedButNotInstalled_returnsTrue() {
        AppUpdateInfo info = mock(AppUpdateInfo.class);
        when(info.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_AVAILABLE);
        when(info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)).thenReturn(true);
        when(info.installStatus()).thenReturn(InstallStatus.DOWNLOADED);

        assertTrue(AppUpdatePromptHelper.shouldShowUpdateDialog(info));
    }
}
