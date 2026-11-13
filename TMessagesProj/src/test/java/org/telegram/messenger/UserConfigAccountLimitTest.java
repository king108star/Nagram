package org.telegram.messenger;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.util.SparseArray;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.telegram.tgnet.TLRPC;

import java.lang.reflect.Field;
import java.util.concurrent.CopyOnWriteArraySet;

@RunWith(RobolectricTestRunner.class)
// API 36 requires Java 21 in Robolectric; CI uses Java 17.
@Config(sdk = 35, application = android.app.Application.class)
public class UserConfigAccountLimitTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        ApplicationLoader.applicationContext = context;
        resetState();
    }

    @After
    public void tearDown() throws Exception {
        resetState();
    }


    @Test
    public void slotIsAssignedBelowDefaultLimit() {
        activateAccounts(UserConfig.MAX_ACCOUNT_DEFAULT_COUNT - 1, false);

        assertEquals(128, UserConfig.MAX_ACCOUNT_COUNT);
        assertEquals(20, UserConfig.getMaxAccountCount());
        assertEquals(UserConfig.MAX_ACCOUNT_DEFAULT_COUNT - 1, UserConfig.requestAccountSlot());
    }

    @Test
    public void slotIsAssignedBelowPremiumLimit() {
        activateAccounts(UserConfig.MAX_ACCOUNT_PREMIUM_COUNT - 1, true);

        assertEquals(20, UserConfig.getMaxAccountCount());
        assertEquals(UserConfig.MAX_ACCOUNT_PREMIUM_COUNT - 1, UserConfig.requestAccountSlot());
    }

    @Test
    public void slotIsRejectedAtDefaultLimit() {
        activateAccounts(UserConfig.MAX_ACCOUNT_DEFAULT_COUNT, false);

        assertEquals(-1, UserConfig.requestAccountSlot());
    }

    @Test
    public void slotIsRejectedAtPremiumLimit() {
        activateAccounts(UserConfig.MAX_ACCOUNT_PREMIUM_COUNT, true);

        assertEquals(-1, UserConfig.requestAccountSlot());
    }

    private void activateAccounts(int count, boolean premium) {
        for (int account = 0; account < count; account++) {
            TLRPC.TL_user user = new TLRPC.TL_user();
            user.id = account + 1;
            user.premium = premium && account == 0;
            UserConfig.getInstance(account).setCurrentUser(user);
            SharedConfig.activeAccounts.add(account);
        }
    }

    private void resetState() throws Exception {
        SharedConfig.activeAccounts = new CopyOnWriteArraySet<>();
        clearSparseArray(UserConfig.class, "Instance");
        clearSparseArray(AccountInstance.class, "Instance");
    }

    private static void clearSparseArray(Class<?> owner, String fieldName) throws Exception {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((SparseArray<?>) field.get(null)).clear();
    }
}
