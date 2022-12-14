
import androidx.fragment.app.FragmentManager
import com.thales.dis.mobile.idcloud.auth.ui.AuthenticatorDescriptionCallback
import com.thales.dis.mobile.idcloud.authui.callback.SampleCommonUiCallback
import com.thalesgroup.gemalto.IdCloudAccessSample.ui.dialog.FriendlyNameFragment

class SampleUiCallback(val fragmentManager: FragmentManager?) : SampleCommonUiCallback(
    fragmentManager
) {

    override fun onAuthenticatorDescription(name: String?, authenticatorDescriptionCallback: AuthenticatorDescriptionCallback?) {
        if (fragmentManager != null) {
            val friendlyNameDialogFragment = authenticatorDescriptionCallback?.let {
                FriendlyNameFragment.newInstance(
                    it
                )
            }
            friendlyNameDialogFragment?.isCancelable = false
            if (!fragmentManager.isDestroyed) {
                friendlyNameDialogFragment?.show(this.fragmentManager, "fragment_friendly_name")
            }
        }
    }
}
