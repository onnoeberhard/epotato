import UIKit
import UserNotifications

class MainController: UITabBarController {

    let ud = UserDefaults.standard

    static var mc: MainController?

    override func viewDidLoad() {
        super.viewDidLoad()
        MainController.mc = self
        tabBar.unselectedItemTintColor = UIColor(red: 0.00, green: 0.00, blue: 0.00, alpha: 0.4)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        if ud.value(forKey: LocalDatabaseHandler.UID) == nil {
            MainController.gotoWelcome(self)
        } else if ud.value(forKey: LocalDatabaseHandler.OLD_UID) != nil && ud.value(forKey: LocalDatabaseHandler.OLD_UID) as! Int != ud.value(forKey: LocalDatabaseHandler.UID) as! Int {
            let alert = UIAlertController(title: "New User", message: "You logged in as a new user! Would you like to continue? Everything of the old user will be deleted!", preferredStyle: UIAlertControllerStyle.alert)
            alert.addAction(UIAlertAction(title: "Continue", style: UIAlertActionStyle.default) { action in
                LocalDatabaseHandler().dropEverything()
                self.ud.removeObject(forKey: LocalDatabaseHandler.OLD_UID)
                self.ud.synchronize()
                self.gotoMain()
            })
            alert.addAction(UIAlertAction(title: "Log Out", style: UIAlertActionStyle.default) { action in
                AppDelegate.logout(self)
            })
            self.present(alert, animated: true, completion: nil)
        } else {
            if #available(iOS 10.0, *) {
                let authOptions: UNAuthorizationOptions = [.alert, .sound]
                UNUserNotificationCenter.current().requestAuthorization(
                        options: authOptions,
                        completionHandler: { _, _ in })
            } else {
                UIApplication.shared.registerUserNotificationSettings(UIUserNotificationSettings(types: [.alert, .sound], categories: nil))
            }
            UIApplication.shared.registerForRemoteNotifications()
            gotoMain()
        }
    }

    static func gotoWelcome(_ vc: UIViewController) {
        vc.present(UIStoryboard(name: "Welcome", bundle: nil).instantiateViewController(withIdentifier: "welcomeController") as UIViewController, animated: false, completion: nil)
    }

    func gotoMain() {
        EndpointsHandler().login(callback: { notice in
            if let notice = notice, notice.code == EndpointsHandler.NOTICE_ERROR || notice.code == EndpointsHandler.NOTICE_NULL {
                AppDelegate.logout(self)
            } else {
                AppDelegate.updateFIEPID()
            }
        }, epid: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int), password: ud.value(forKey: LocalDatabaseHandler.PASSWORD) as! String)
    }

}
