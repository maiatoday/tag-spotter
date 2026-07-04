package net.maiatoday.tagspotter.feature.settings.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIViewController

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    return remember {
        object : ToastLauncher {
            override fun showToast(message: String) {
                println("iOS Toast alert: $message")
                val window = UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
                val rootViewController = window?.rootViewController
                if (rootViewController != null) {
                    val alert = UIAlertController.alertControllerWithTitle(
                        title = "Tag Spotter",
                        message = message,
                        preferredStyle = UIAlertControllerStyleAlert
                    )
                    alert.addAction(
                        UIAlertAction.actionWithTitle(
                            title = "OK",
                            style = UIAlertActionStyleDefault,
                            handler = null
                        )
                    )
                    
                    var topController: UIViewController = rootViewController
                    while (topController.presentedViewController != null) {
                        topController = topController.presentedViewController!!
                    }
                    topController.presentViewController(alert, animated = true, completion = null)
                }
            }
        }
    }
}
