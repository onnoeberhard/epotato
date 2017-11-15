import UIKit
import UserNotifications
import Firebase
import IQKeyboardManagerSwift
import Contacts
import PhoneNumberKit
import GoogleMobileAds

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        if let options = launchOptions, let notification = options[UIApplicationLaunchOptionsKey.remoteNotification] as? [NSObject: AnyObject] {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.01) {
                self.application(application, didReceiveRemoteNotification: notification)
            }
        }
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        if #available(iOS 10.0, *) {
            UNUserNotificationCenter.current().delegate = self
        }
        application.registerForRemoteNotifications()
        IQKeyboardManager.sharedManager().enable = true
        GADMobileAds.configure(withApplicationID: Credentials.GAD_APP_ID)
        return true
    }

    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable: Any]) {
        handleFCM(userInfo) {
        }
    }

    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable: Any],
                     fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        handleFCM(userInfo) {
            completionHandler(UIBackgroundFetchResult.newData)
        }
    }

    func handleFCM(_ fcm: [AnyHashable: Any], _ completed: @escaping () -> ()) {
        let ldb = LocalDatabaseHandler()
        if let type = fcm["type"] as? String, Int(type)! == EndpointsHandler.TYPE_POTATO && !ldb.getAll(table: ldb.receivedPotatoes, column: ldb.id).contains(where: { $0 == fcm["pid"] as? String }) {
            var message = fcm["message"] as! String
            let iv = message.substring(from: message.index(message.endIndex, offsetBy: -16))
            message = CryptLib().decryptCipherText(with: message.substring(to: message.index(message.endIndex, offsetBy: -16)), key: Credentials.KEY, iv: iv)
            ldb.insert(table: ldb.receivedPotatoes, pairs: [
                    LocalDatabaseHandler.ID, fcm["pid"] as? String,
                    LocalDatabaseHandler.DT, LocalDatabaseHandler.getTimestamp(),
                    LocalDatabaseHandler.UID, fcm["uid"] as? String,
                    LocalDatabaseHandler.RECEIVED_POTATOES_TS, fcm["ts"] as? String,
                    LocalDatabaseHandler.POTATO_TEXT, message,
                    LocalDatabaseHandler.POTATO_FORM, fcm["form"] as? String
            ])
            ldb.insert(table: ldb.newPotatoes, pairs: [LocalDatabaseHandler.NEW_POTATOES_PID, fcm["pid"] as? String])
            if ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: fcm["uid"] as! String, column: ldb.id) == nil &&
                       ldb.get(table: ldb.tempContacts, idKey: ldb.uid, idValue: fcm["uid"] as! String, column: ldb.id) == nil {
                EndpointsHandler().getProfile(callback: { profile in
                    if let profile = profile {
                        ldb.insert(table: ldb.tempContacts, pairs: [
                                LocalDatabaseHandler.UID, profile.identifier!.stringValue,
                                LocalDatabaseHandler.EPID, profile.epid!
                        ])
                        self.updatePotatoes()
                        completed()
                    } else {
                        EndpointsHandler().getProfile(callback: { profile in
                            if let profile = profile {
                                ldb.insert(table: ldb.tempContacts, pairs: [
                                        LocalDatabaseHandler.UID, profile.identifier!.stringValue,
                                        LocalDatabaseHandler.EPID, profile.epid!
                                ])
                            }
                            self.updatePotatoes()
                            completed()
                        }, key: EndpointsHandler.ID, value: fcm["uid"] as! String)
                    }
                }, key: EndpointsHandler.ID, value: fcm["uid"] as! String)
            } else {
                self.updatePotatoes()
                completed()
            }
        } else if let type = fcm["type"] as? String, type == String(EndpointsHandler.TYPE_FEED_POTATO) && !ldb.getAll(table: ldb.feedPotatoes, column: ldb.id).contains(where: { $0 == fcm["pid"] as? String }) {
            var message = fcm["message"] as! String
            let iv = message.substring(from: message.index(message.endIndex, offsetBy: -16))
            message = CryptLib().decryptCipherText(with: message.substring(to: message.index(message.endIndex, offsetBy: -16)), key: Credentials.KEY, iv: iv)
            ldb.insert(table: ldb.feedPotatoes, pairs: [
                    LocalDatabaseHandler.ID, fcm["pid"] as? String,
                    LocalDatabaseHandler.DT, LocalDatabaseHandler.getTimestamp(),
                    LocalDatabaseHandler.UID, fcm["uid"] as? String,
                    LocalDatabaseHandler.POTATO_TEXT, message,
                    LocalDatabaseHandler.POTATO_FORM, fcm["form"] as? String
            ])
            ldb.insert(table: ldb.newFeedPotatoes, pairs: [LocalDatabaseHandler.NEW_POTATOES_PID, fcm["pid"] as? String])
            if ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: fcm["uid"] as! String, column: ldb.id) == nil &&
                       ldb.get(table: ldb.following, idKey: ldb.uid, idValue: fcm["uid"] as! String, column: ldb.id) == nil &&
                       ldb.get(table: ldb.tempContacts, idKey: ldb.uid, idValue: fcm["uid"] as! String, column: ldb.id) == nil {
                EndpointsHandler().getProfile(callback: { profile in
                    if let profile = profile {
                        ldb.insert(table: ldb.tempContacts, pairs: [
                                LocalDatabaseHandler.UID, profile.identifier!.stringValue,
                                LocalDatabaseHandler.EPID, profile.epid!
                        ])
                        self.updateFeedPotatoes()
                        completed()
                    } else {
                        EndpointsHandler().getProfile(callback: { profile in
                            if let profile = profile {
                                ldb.insert(table: ldb.tempContacts, pairs: [
                                        LocalDatabaseHandler.UID, profile.identifier!.stringValue,
                                        LocalDatabaseHandler.EPID, profile.epid!
                                ])
                            }
                            self.updateFeedPotatoes()
                            completed()
                        }, key: EndpointsHandler.ID, value: fcm["uid"] as! String)
                    }
                }, key: EndpointsHandler.ID, value: fcm["uid"] as! String)
            } else {
                self.updateFeedPotatoes()
                completed()
            }
        } else {
            completed()
        }
    }

    func updatePotatoes() {
        if let rc = ReceivedController.rc {
            rc.update({})
            rc.notify()
        }
        if let fc = FeedController.fc {
            fc.notify()
        }
        if let sc = SentController.sc {
            sc.notify()
        }
    }

    func updateFeedPotatoes() {
        if let rc = ReceivedController.rc {
            rc.notifyFeed()
        }
        if let fc = FeedController.fc {
            fc.update({})
            fc.notifyFeed()
        }
        if let sc = SentController.sc {
            sc.notify()
        }
    }

    static func updateFIEPID(_ completed: (() -> ())? = nil) {
        let ud = UserDefaults.standard
        if ud.value(forKey: LocalDatabaseHandler.UID) != nil {
            EndpointsHandler().getProfile(callback: { profile in
                if let profile = profile {
                    ud.set(profile.epid, forKey: LocalDatabaseHandler.EPID)
                    ud.set(profile.phone == nil || profile.phone == EndpointsHandler.NULL ? nil : profile.phone, forKey: LocalDatabaseHandler.PHONE)
                    ud.set(profile.followers != nil ? profile.followers!.count : 0, forKey: LocalDatabaseHandler.FOLLOWERS)
                    ud.set(profile.strangers, forKey: LocalDatabaseHandler.TOTAL_STRANGERS)
                    if let fiid = Messaging.messaging().fcmToken, profile.fiidsIos == nil || !profile.fiidsIos!.contains(fiid) {
                        var fiids = profile.fiidsIos ?? [String]()
                        if let index = fiids.index(of: ud.value(forKey: LocalDatabaseHandler.FIID) as? String ?? "") {
                            fiids.remove(at: index)
                        }
                        ud.set(fiid, forKey: LocalDatabaseHandler.FIID)
                        fiids.append(fiid)
                        EndpointsHandler().serialize(callback: { notice in
                            if let notice = notice, let x = notice.message, x != "" {
                                EndpointsHandler().update(callback: { id in }, kind: EndpointsHandler.PROFILE, property: EndpointsHandler.ID,
                                        value: profile.identifier!.stringValue, properties: [EndpointsHandler.P_FIIDS_IOS], values: [x])
                            }
                        }, input: fiids)
                    }
                    if let potatoes = profile.potatoes, potatoes.count > 0 {
                        EndpointsHandler().getPotatoes(callback: { potatoes in
                            if let pc = potatoes, let ps = pc.items {
                                let ldb = LocalDatabaseHandler()
                                for p in ps {
                                    var message = p.message
                                    let cryptoLib = CryptLib();
                                    let iv = message!.substring(from: message!.index(message!.endIndex, offsetBy: -16))
                                    message = cryptoLib.decryptCipherText(with: message!.substring(to: message!.index(message!.endIndex, offsetBy: -16)), key: Credentials.KEY, iv: iv)
                                    ldb.insert(table: ldb.receivedPotatoes, pairs: [
                                            LocalDatabaseHandler.ID, p.pid,
                                            LocalDatabaseHandler.DT, LocalDatabaseHandler.getTimestamp(),
                                            LocalDatabaseHandler.UID, p.uid,
                                            LocalDatabaseHandler.RECEIVED_POTATOES_TS, String(false),
                                            LocalDatabaseHandler.POTATO_TEXT, message,
                                            LocalDatabaseHandler.POTATO_FORM, p.form!.stringValue
                                    ])
                                    if ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: p.uid!, column: ldb.id) == nil &&
                                               ldb.get(table: ldb.tempContacts, idKey: ldb.uid, idValue: p.uid!, column: ldb.id) == nil {
                                        EndpointsHandler().getProfile(callback: { profile in
                                            if let profile = profile {
                                                ldb.insert(table: ldb.tempContacts, pairs: [
                                                        LocalDatabaseHandler.UID, profile.identifier!.stringValue,
                                                        LocalDatabaseHandler.EPID, profile.epid!
                                                ])
                                            }
                                        }, key: EndpointsHandler.ID, value: p.uid!)
                                    }
                                }
                                completed?()
                            } else {
                                completed?()
                            }
                        }, uid: profile.identifier!.stringValue);
                    } else {
                        completed?()
                    }
                } else {
                    completed?()
                }
            }, key: EndpointsHandler.ID, value: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int))
        }
    }

    static func contactsPermission(_ vc: UIViewController, _ completed: @escaping (_: Bool, _: (() -> ())?) -> ()) {
        let authorizationStatus = CNContactStore.authorizationStatus(for: CNEntityType.contacts)
        switch authorizationStatus {
        case .authorized:
            completed(true, nil)
            break
        case .denied, .notDetermined:
            CNContactStore().requestAccess(for: CNEntityType.contacts, completionHandler: { (access, accessError) -> Void in
                if authorizationStatus == CNAuthorizationStatus.denied {
                    completed(access) {
                        vc.display("Please allow the app to access your contacts through the System Settings.\nIf you know the ePotato-IDs of your friends, you can also add them with the button up there ↗️")
                    }
                }
                completed(access, nil)
            })
            break
        default:
            completed(false, nil)
            break
        }
    }

    static func loadContacts(_ vc: UIViewController, _ completed: @escaping () -> (), showWarning: Bool = true) {
        contactsPermission(vc) { access, disp in
            var numbernames = [String: String]()
            if access {
                let group = DispatchGroup()
                group.enter()
                DispatchQueue.main.async {
                    do {
                        let formatter = CNContactFormatter()
                        formatter.style = .fullName
                        let numberKit = PhoneNumberKit()
                        try CNContactStore().enumerateContacts(with: CNContactFetchRequest(keysToFetch: [CNContactFormatter.descriptorForRequiredKeys(for: .fullName), CNContactPhoneNumbersKey as CNKeyDescriptor])) {
                            (contact, stop) in
                            if contact.phoneNumbers.count > 0 {
                                if let name = formatter.string(from: contact), let number = try? numberKit.parse(contact.phoneNumbers[0].value.stringValue) {
                                    numbernames[numberKit.format(number, toType: .e164, withPrefix: true)] = name
                                }
                            }
                        }
                    } catch {
                    }
                    group.leave()
                }
                group.notify(queue: .main) {
                    loadMoreContacts(vc, numbernames) {
                        completed()
                        if showWarning {
                            disp?()
                        }
                    }
                }
            } else {
                loadMoreContacts(vc, numbernames) {
                    completed()
                    if showWarning {
                        disp?()
                    }
                }
            }
        }
    }

    static func loadMoreContacts(_ vc: UIViewController, _ numbernames: [String: String], _ completed: @escaping () -> ()) {
        let group = DispatchGroup()
        group.enter()
        DispatchQueue.main.async {
            let ldb = LocalDatabaseHandler()
            let contactIds = ldb.getAll(table: ldb.contacts, column: ldb.uid)
            EndpointsHandler().getContacts(callback: { collection in
                if let ps = collection?.items {
                    // Delete all expired contacts from ldb
                    for cid in contactIds {
                        var keep = false;
                        for p: GTLRPotatoAPI_Profile in ps {
                            if p.identifier!.stringValue == cid {
                                keep = true;
                                break;
                            }
                        }
                        if !keep {
                            ldb.inup(table: ldb.tempContacts, pairs: [LocalDatabaseHandler.UID, cid, LocalDatabaseHandler.EPID, ldb.get(table: ldb.contacts, idKey: ldb.id, idValue: cid!, column: ldb.epid)])
                            ldb.delete(table: ldb.contacts, idKey: LocalDatabaseHandler.UID, idValue: cid!)
                        }
                    }
                    // Delete phone number & name from existing linked contacts -> in case of new phone number: keep contact, but not linked
                    for c in ldb.getAll(table: ldb.contacts) {
                        if c[LocalDatabaseHandler.CONTACT_PHONE] != nil {
                            ldb.inup(table: ldb.contacts, pairs: [LocalDatabaseHandler.ID, c[LocalDatabaseHandler.ID]!!, LocalDatabaseHandler.CONTACT_NAME, nil, LocalDatabaseHandler.CONTACT_PHONE, nil])
                        }
                    }
                    // Add all new contacts to ldb
                    var newContacts = [String]()
                    var newContactIds = [String]()
                    var used_names = [String]()
                    for p: GTLRPotatoAPI_Profile in ps {
                        if let phone = p.phone, !contactIds.contains(where: { $0 == p.identifier!.stringValue }) && numbernames.keys.contains(where: { $0 == phone }) {
                            newContacts.append(numbernames[phone]!)
                            newContactIds.append(p.identifier!.stringValue)
                        }
                        if !contactIds.contains(where: { $0 == p.identifier!.stringValue }) {
                            ldb.inup(table: ldb.contacts, pairs: [LocalDatabaseHandler.UID, p.identifier!.stringValue, LocalDatabaseHandler.EPID, p.epid])
                        }
                        if let phone = p.phone, numbernames.keys.contains(where: { $0 == phone }) {
                            ldb.inup(table: ldb.contacts, pairs: [LocalDatabaseHandler.UID, p.identifier!.stringValue, LocalDatabaseHandler.EPID, p.epid, LocalDatabaseHandler.CONTACT_PHONE, phone, LocalDatabaseHandler.CONTACT_NAME, numbernames[phone]])
                            used_names.append(numbernames[phone]!)
                        }
                    }
                    // Delete all contacts that are now in real contacts from phone contacts list
                    ldb.truncate(ldb.phoneContacts)
                    for numbername in numbernames {
                        if !used_names.contains(numbername.value) && !ldb.getAll(table: ldb.phoneContacts, column: ldb.contactName).contains(where: { $0 == numbername.value }) {
                            ldb.insert(table: ldb.phoneContacts, pairs: [LocalDatabaseHandler.CONTACT_NAME, numbername.value, LocalDatabaseHandler.CONTACT_PHONE, numbername.key])
                        }
                    }
                }
                // Update Followers, Following
                EndpointsHandler().getFollowing { collection in
                    if let ps = collection?.items, ps.count > 0 {
                        ldb.truncate(ldb.following)
                        for p in ps {
                            ldb.inup(table: ldb.following, pairs: [LocalDatabaseHandler.UID, p.identifier!.stringValue, LocalDatabaseHandler.EPID, p.epid])
                        }
                    }
                }
                // Get & update suggested following
                EndpointsHandler().followSuggestions { collection in
                    if let ps = collection?.items, ps.count > 0 {
                        ldb.truncate(ldb.suggestedFollowing)
                        var suggestions = [GTLRPotatoAPI_Profile: Int]()
                        for p: GTLRPotatoAPI_Profile in ps {
                            if suggestions.keys.contains(p) {
                                suggestions[p] = suggestions[p]! + 1
                            } else {
                                suggestions[p] = 1
                            }
                        }
                        for s in suggestions {
                            if s.key.identifier as! Int != UserDefaults.standard.value(forKey: LocalDatabaseHandler.UID) as! Int {
                                ldb.insert(table: ldb.suggestedFollowing, pairs: [LocalDatabaseHandler.UID, s.key.identifier!.stringValue, LocalDatabaseHandler.EPID, s.key.epid, LocalDatabaseHandler.CONTACTS_SCORE, String(s.value)])
                            }
                        }
                    }
                }
                group.leave()

            }, numbers: Array(numbernames.keys))
        }
        group.notify(queue: .main) {
            completed()
        }
    }

    static func logout(_ vc: UIViewController) {
        let ud = UserDefaults.standard
        let epid = String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int)
        EndpointsHandler().login(callback: { notice in
            if let n = notice, n.ok!.boolValue {
                EndpointsHandler().update(callback: { id in }, kind: EndpointsHandler.PROFILE, property: EndpointsHandler.ID, value: epid, properties: [EndpointsHandler.P_FIIDS_IOS], values: [""])
            }
        }, epid: epid, password: ud.value(forKey: LocalDatabaseHandler.PASSWORD) as! String)
        ud.set(ud.value(forKey: LocalDatabaseHandler.UID), forKey: LocalDatabaseHandler.OLD_UID)
        ud.removeObject(forKey: LocalDatabaseHandler.UID)
        ud.removeObject(forKey: LocalDatabaseHandler.EPID)
        ud.removeObject(forKey: LocalDatabaseHandler.PASSWORD)
        ud.synchronize()
        InstanceID.instanceID().deleteID { (e) in
        }
        MainController.gotoWelcome(vc)
    }

}

extension UIViewController {
    func display(_ msg: String, again: (() -> ())? = nil, completed: (() -> ())? = nil) {
        let warning = UIAlertController(title: nil, message: msg, preferredStyle: UIAlertControllerStyle.alert)
        if let again = again {
            warning.addAction(UIAlertAction(title: "Again", style: UIAlertActionStyle.default) { action in
                again()
            })
        }
        warning.addAction(UIAlertAction(title: again == nil ? "OK" : "Cancel", style: UIAlertActionStyle.cancel) { action in
            completed?()
        })
        self.present(warning, animated: true)
    }
}

@available(iOS 10, *)
extension AppDelegate: UNUserNotificationCenterDelegate {
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let userInfo = notification.request.content.userInfo
        handleFCM(userInfo) {
            completionHandler([])
        }
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        handleFCM(userInfo) {
            completionHandler()
        }
    }
}

extension AppDelegate: MessagingDelegate {
    func messaging(_ messaging: Messaging, didRefreshRegistrationToken fcmToken: String) {
        if UserDefaults.standard.value(forKey: LocalDatabaseHandler.UID) != nil {
            AppDelegate.updateFIEPID()
        }
    }

    func messaging(_ messaging: Messaging, didReceive remoteMessage: MessagingRemoteMessage) {
        handleFCM(remoteMessage.appData) {
        }
    }
}

extension String {
    var localized: String {
        return NSLocalizedString(self, tableName: nil, bundle: Bundle.main, value: "", comment: "")
    }

    func sha256() -> String {
        if let stringData = self.data(using: String.Encoding.utf8) as NSData? {
            let digestLength = Int(CC_SHA256_DIGEST_LENGTH)
            var hash = [UInt8](repeating: 0, count: digestLength)
            CC_SHA256(stringData.bytes, UInt32(stringData.length), &hash)
            let digest = NSData(bytes: hash, length: digestLength)
            var bytes = [UInt8](repeating: 0, count: digest.length)
            digest.getBytes(&bytes, length: digest.length)
            var hexString = ""
            for byte in bytes {
                hexString += String(format: "%02x", UInt8(byte))
            }
            return hexString
        }
        return ""
    }

    func matches(_ regex: String) -> Bool {
        return self.range(of: regex, options: .regularExpression, range: nil, locale: nil) != nil
    }
}
