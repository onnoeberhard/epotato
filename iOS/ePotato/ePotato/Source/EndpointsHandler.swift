class EndpointsHandler {

    static let ID = "id";

    static let PROFILE = "profile";
    static let P_CONTACTS = "contacts";
    static let P_EPID = "epid";
    static let P_FIIDS = "fiids";
    static let P_FIIDS_IOS = "fiids_ios";
    static let P_FOLLOWERS = "followers";
    static let P_FOLLOWING = "following";
    static let P_PASSWORD = "password";
    static let P_PHONE = "phone";
    static let P_RAND = "rand";
    static let P_STRANGERS = "strangers";
    static let TYPE_POTATO = 1;
    static let TYPE_FEED_POTATO = 2;

    static let NULL = "*NULL*"

    static let NOTICE_OK: NSNumber = 1
    static let NOTICE_ERROR: NSNumber = 2
    static let NOTICE_OTHER: NSNumber = 3
    static let NOTICE_NULL: NSNumber = 4

    let service = GTLRPotatoAPIService()

    let ud = UserDefaults.standard

    init() {
        service.isRetryEnabled = true
    }

    func check(callback: @escaping (GTLRPotatoAPI_Id?) -> Void, kind: String, key: String, value: String) {
        service.executeQuery(GTLRPotatoAPIQuery_Check.query(withXKind: kind, idKey: key, idValue: value)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Id)
        }
    }

    func getProfile(callback: @escaping (GTLRPotatoAPI_Profile?) -> Void, key: String, value: String) {
        service.executeQuery(GTLRPotatoAPIQuery_GetProfile.query(withIdKey: key, idValue: value)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Profile)
        }
    }

    func getPotatoes(callback: @escaping (GTLRPotatoAPI_PotatoCollection?) -> Void, uid: String) {
        service.executeQuery(GTLRPotatoAPIQuery_GetPotatoes.query(withUid: uid)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_PotatoCollection)
        }
    }

    func update(callback: @escaping (GTLRPotatoAPI_Id?) -> Void, kind: String, property: String, value: String, properties: [String], values: [String]) {
        service.executeQuery(GTLRPotatoAPIQuery_Update.query(withXKind: kind, idKey: property, idValue: value, properties: properties, values: values)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Id)
        }
    }

    func sms(callback: @escaping (GTLRPotatoAPI_Notice?) -> Void, number: String, body: String) {
        service.executeQuery(GTLRPotatoAPIQuery_Sms.query(withNumber: number, body: body)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Notice)
        }
    }

    func signup(callback: @escaping (GTLRPotatoAPI_Id?) -> Void, epid: String, password: String, phone: String) {
        service.executeQuery(GTLRPotatoAPIQuery_Signup.query(withEpid: epid, password: password, phone: phone)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Id)
        }
    }

    func login(callback: @escaping (GTLRPotatoAPI_Notice?) -> Void, epid: String, password: String) {
        service.executeQuery(GTLRPotatoAPIQuery_Login.query(withEpid: epid, password: password)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Notice)
        }
    }

    func recoverPassword(callback: @escaping (GTLRPotatoAPI_Notice?) -> Void, epid: String) {
        service.executeQuery(GTLRPotatoAPIQuery_RecoverPassword.query(withEpid: epid)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Notice)
        }
    }

    func changePassword(callback: @escaping (GTLRPotatoAPI_Notice?) -> Void, epid: String, npw: String, opw: String) {
        service.executeQuery(GTLRPotatoAPIQuery_ChangePassword.query(withEpid: epid, npw: npw, opw: opw)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Notice)
        }
    }

    func deleteAccount(callback: @escaping (GTLRPotatoAPI_Notice?) -> Void, epid: String, pw: String) {
        service.executeQuery(GTLRPotatoAPIQuery_DeleteAccount.query(withEpid: epid, password: pw)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Notice)
        }
    }

    func sendPotato(callback: @escaping (GTLRPotatoAPI_Notice?) -> Void, uids: [String], ts: [Bool], message: String, type: Int, pid: String) {
        var msg = message
        let crypter = CryptLib();
        let iv = crypter.generateRandomIV(16)!
        msg = (crypter.encryptPlainText(with: msg, key: Credentials.KEY, iv: iv)! as String) + iv
        service.executeQuery(GTLRPotatoAPIQuery_SendPotato.query(withXAddresseesUids: uids, addresseesTs: ts as [NSNumber], addressor: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int), form: type, message: message, pid: pid)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Notice)
        }
    }

    func getTS(callback: @escaping (GTLRPotatoAPI_ProfileCollection?) -> Void, n: Int) {
        let ldb = LocalDatabaseHandler()
        var contacts = ldb.getAll(table: ldb.contacts, column: ldb.uid)
        contacts.append(contentsOf: ldb.getAll(table: ldb.tempContacts, column: ldb.uid))
        contacts.append(String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int))
        service.executeQuery(GTLRPotatoAPIQuery_GetTS.query(withXN: n, contacts: contacts as! [String])) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_ProfileCollection)
        }
    }

    func newContact(callback: @escaping (GTLRPotatoAPI_Notice?) -> Void, uid: String) {
        service.executeQuery(GTLRPotatoAPIQuery_NewContact.query(withUid: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int), contactUid: uid)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Notice)
        }
    }

    func deleteContact(callback: @escaping (GTLRPotatoAPI_Notice?) -> Void, uid: String) {
        service.executeQuery(GTLRPotatoAPIQuery_DeleteContact.query(withUid: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int), contactUid: uid)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Notice)
        }
    }

    func getContacts(callback: @escaping (GTLRPotatoAPI_ProfileCollection?) -> Void, numbers: [String]) {
        var n = numbers
        if n.count == 0 {
            n = ["*NULL*"]
        }
        service.executeQuery(GTLRPotatoAPIQuery_GetContacts.query(withXUid: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int), phoneNumbers: n)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_ProfileCollection)
        }
    }

    func contactSuggestions(callback: @escaping (GTLRPotatoAPI_ProfileCollection?) -> Void) {
        service.executeQuery(GTLRPotatoAPIQuery_ContactSuggestions.query(withUid: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int))) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_ProfileCollection)
        }
    }

    func followSuggestions(callback: @escaping (GTLRPotatoAPI_ProfileCollection?) -> Void) {
        service.executeQuery(GTLRPotatoAPIQuery_FollowSuggestions.query(withUid: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int))) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_ProfileCollection)
        }
    }

    func getFollowing(callback: @escaping (GTLRPotatoAPI_ProfileCollection?) -> Void) {
        service.executeQuery(GTLRPotatoAPIQuery_GetFollowing.query(withUid: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int))) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_ProfileCollection)
        }
    }

    func follow(callback: @escaping (GTLRPotatoAPI_Notice?) -> Void, uid: String) {
        service.executeQuery(GTLRPotatoAPIQuery_Follow.query(withXUid: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int), newId: uid)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Notice)
        }
    }

    func unfollow(callback: @escaping (GTLRPotatoAPI_Notice?) -> Void, uid: String) {
        service.executeQuery(GTLRPotatoAPIQuery_Unfollow.query(withXUid: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int), oldId: uid)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Notice)
        }
    }
    
    func serialize(callback: @escaping (GTLRPotatoAPI_Notice?) -> Void, input: [String]) {
        service.executeQuery(GTLRPotatoAPIQuery_Serialize.query(withInput: input)) {
            ticket, any, error in
            callback(any as? GTLRPotatoAPI_Notice)
        }
    }
}
