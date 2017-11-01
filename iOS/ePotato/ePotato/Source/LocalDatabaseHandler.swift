import SQLite

class LocalDatabaseHandler {

    static let UID = "uid"
    static let EPID = "epid"
    static let PASSWORD = "password"
    static let PHONE = "phone"
    static let FIID = "fiid"
    static let OLD_UID = "old_uid"
    static let TOTAL_STRANGERS = "total_strangers"
    static let CREEPS = "creeps"
    static let FEED_NOTIFICATIONS = "feed_notifications"
    static let FOLLOWERS = "followers"

    static let PREMIUM = "premium"
    static let RESTORED = "restored"

    static let ID = "id"
    static let DT = "dt"
    static let CONTACT_NAME = "name"
    static let CONTACT_PHONE = "phone"
    static let POTATO_TEXT = "text"
    static let POTATO_FORM = "form"

    static let CONTACTS_SCORE = "score"
    static let RECEIVED_POTATOES_TS = "ts"
    static let NEW_POTATOES_PID = "pid"
    static let SENT_POTATOES_UIDS = "uids"
    static let SENT_POTATOES_TSS = "tss"
    static let SENT_POTATOES_NAMES = "names"
    static let SENT_POTATOES_CODE = "code"

    let id = Expression<String?>(ID)
    let uid = Expression<String?>(UID)
    let epid = Expression<String?>(EPID)
    let dt = Expression<String?>(DT)
    let contactName = Expression<String?>(CONTACT_NAME)
    let contactPhone = Expression<String?>(CONTACT_PHONE)
    let potatoText = Expression<String?>(POTATO_TEXT)
    let potatoForm = Expression<String?>(POTATO_FORM)

    let contactsScore = Expression<String?>(CONTACTS_SCORE)
    let receivedPotatosTs = Expression<String?>(RECEIVED_POTATOES_TS)
    let newPotatoesPid = Expression<String?>(NEW_POTATOES_PID)
    let sentPotatoesUids = Expression<String?>(SENT_POTATOES_UIDS)
    let sentPotatoesTss = Expression<String?>(SENT_POTATOES_TSS)
    let sentPotatoesNames = Expression<String?>(SENT_POTATOES_NAMES)
    let sentPotatoesCode = Expression<String?>(SENT_POTATOES_CODE)
    static let SENT_POTATOES_CODE_NULL = 0
    static let SENT_POTATOES_CODE_SENDING = 1
    static let SENT_POTATOES_CODE_ERROR = 2
    static let SENT_POTATOES_CODE_SENT = 3
    static let SENT_POTATOES_CODE_RECEIVED = 4

    var columns = [String: Expression<String?>]()

    let contacts = Table("contacts")
    let receivedPotatoes = Table("received_potatoes")
    let sentPotatoes = Table("sent_potatoes")
    let newPotatoes = Table("new_potatoes")
    let phoneContacts = Table("phone_contacts")
    let tempContacts = Table("temp_contacts")
    let suggestedContacts = Table("suggested_contacts")
    let suggestedFollowing = Table("suggested_following")
    let feedPotatoes = Table("feed_potatoes")
    let newFeedPotatoes = Table("new_feed_potatoes")
    let following = Table("following")

    private static let DELETE = "*DELETE*"

    let db: Connection

    init() {
        let path = NSSearchPathForDirectoriesInDomains(
                .documentDirectory, .userDomainMask, true
        ).first!
        db = try! Connection("\(path)/db.sqlite3")
        try! db.run(contacts.create(ifNotExists: true) { t in
            t.column(id, primaryKey: true)
            t.column(uid)
            t.column(epid)
            t.column(contactName)
            t.column(contactPhone)
            t.column(contactsScore)
        })
        try! db.run(receivedPotatoes.create(ifNotExists: true) { t in
            t.column(id, primaryKey: true)
            t.column(uid)
            t.column(receivedPotatosTs)
            t.column(dt)
            t.column(potatoText)
            t.column(potatoForm)
        })
        try! db.run(sentPotatoes.create(ifNotExists: true) { t in
            t.column(id, primaryKey: true)
            t.column(sentPotatoesUids)
            t.column(sentPotatoesTss)
            t.column(sentPotatoesNames)
            t.column(dt)
            t.column(sentPotatoesCode)
            t.column(potatoText)
            t.column(potatoForm)
        })
        try! db.run(newPotatoes.create(ifNotExists: true) { t in
            t.column(id, primaryKey: true)
            t.column(newPotatoesPid)
        })
        try! db.run(phoneContacts.create(ifNotExists: true) { t in
            t.column(id, primaryKey: true)
            t.column(contactName)
            t.column(contactPhone)
        })
        try! db.run(tempContacts.create(ifNotExists: true) { t in
            t.column(id, primaryKey: true)
            t.column(uid)
            t.column(epid)
        })
        try! db.run(suggestedContacts.create(ifNotExists: true) { t in
            t.column(id, primaryKey: true)
            t.column(uid)
            t.column(epid)
            t.column(contactsScore)
        })
        try! db.run(suggestedFollowing.create(ifNotExists: true) { t in
            t.column(id, primaryKey: true)
            t.column(uid)
            t.column(epid)
            t.column(contactsScore)
        })
        try! db.run(feedPotatoes.create(ifNotExists: true) { t in
            t.column(id, primaryKey: true)
            t.column(uid)
            t.column(dt)
            t.column(potatoText)
            t.column(potatoForm)
        })
        try! db.run(newFeedPotatoes.create(ifNotExists: true) { t in
            t.column(id, primaryKey: true)
            t.column(newPotatoesPid)
        })
        try! db.run(following.create(ifNotExists: true) { t in
            t.column(id, primaryKey: true)
            t.column(uid)
            t.column(epid)
        })
        columns = [
                LocalDatabaseHandler.ID: id,
                LocalDatabaseHandler.UID: uid,
                LocalDatabaseHandler.EPID: epid,
                LocalDatabaseHandler.DT: dt,
                LocalDatabaseHandler.CONTACT_NAME: contactName,
                LocalDatabaseHandler.CONTACT_PHONE: contactPhone,
                LocalDatabaseHandler.POTATO_TEXT: potatoText,
                LocalDatabaseHandler.POTATO_FORM: potatoForm,
                LocalDatabaseHandler.CONTACTS_SCORE: contactsScore,
                LocalDatabaseHandler.RECEIVED_POTATOES_TS: receivedPotatosTs,
                LocalDatabaseHandler.NEW_POTATOES_PID: newPotatoesPid,
                LocalDatabaseHandler.SENT_POTATOES_UIDS: sentPotatoesUids,
                LocalDatabaseHandler.SENT_POTATOES_TSS: sentPotatoesTss,
                LocalDatabaseHandler.SENT_POTATOES_NAMES: sentPotatoesNames,
                LocalDatabaseHandler.SENT_POTATOES_CODE: sentPotatoesCode
        ]
    }

    func dropEverything() {
        truncate(contacts)
        truncate(receivedPotatoes)
        truncate(sentPotatoes)
        truncate(newPotatoes)
        truncate(phoneContacts)
        truncate(tempContacts)
        truncate(suggestedContacts)
        truncate(suggestedFollowing)
        truncate(feedPotatoes)
        truncate(newFeedPotatoes)
        truncate(following)
    }

    func get(table: Table, idKey: Expression<String?>, idValue: String, column: Expression<String?>) -> String? {
        return try! db.scalar(table.select(column).filter(idKey == idValue))
    }

    func getAll(table: Table, idKey: Expression<String?>, idValue: String, column: Expression<String?>) -> [String?] {
        var result = [String?]()
        for row in try! db.prepare(table.select(column).filter(idKey == idValue)) {
            result.append(row[column])
        }
        return result
    }

    func getAll(table: Table, column: Expression<String?>) -> [String?] {
        var result = [String?]()
        for row in try! db.prepare(table.select(column)) {
            result.append(row[column])
        }
        return result
    }

    func getAll(table: Table, idKey: Expression<String?>, idValue: String) -> [[String: String?]] {
        var result = [[String: String?]]()
        for row in try! db.prepare(table.filter(idKey == idValue)) {
            var data = [String: String?]()
            for column in row.columnNames.values {
                var key = Array(row.columnNames.keys)[column] as String
                key = key.substring(with: key.index(key.startIndex, offsetBy: 1)..<key.index(key.endIndex, offsetBy: -1))
                data[key] = try! row.get(columns[key]!)
            }
            result.append(data)
        }
        return result
    }

    func getAll(table: Table) -> [[String: String?]] {
        var result = [[String: String?]]()
        for row in try! db.prepare(table) {
            var data = [String: String?]()
            for column in row.columnNames.values {
                var key = Array(row.columnNames.keys)[column] as String
                key = key.substring(with: key.index(key.startIndex, offsetBy: 1)..<key.index(key.endIndex, offsetBy: -1))
                data[key] = try! row.get(columns[key]!)
            }
            result.append(data)
        }
        return result
    }

    @discardableResult
    fileprivate func inup(insert: Bool, _update: Bool, table: Table, pairs: [String?]) -> String? {
        var result: String?
        let delete = pairs[0] == LocalDatabaseHandler.DELETE || pairs.count > 2 && pairs[2] == LocalDatabaseHandler.DELETE
        let update = !delete && pairs[0] != nil && pairs[0] != "" && get(table: table, idKey: columns[pairs[0]!]!, idValue: pairs[1]!, column: columns[pairs[0]!]!) != nil && !insert
        if delete {
            try! db.run((pairs[0] == LocalDatabaseHandler.DELETE ? table : table.filter(columns[pairs[0]!]! == pairs[1]!)).delete())
        } else if update {
            var set = [Setter]()
            for i in 2..<pairs.count {
                if i % 2 == 0 {
                    set.append(columns[pairs[i]!]! <- pairs[i + 1])
                }
            }
            if pairs.count > 2 {
                try! db.run(table.filter(columns[pairs[0]!]! == pairs[1]!).update(set))
            }
        } else if !_update {
            var set = [Setter]()
            if pairs[0] != LocalDatabaseHandler.ID {
                let B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
                var id: String
                repeat {
                    id = ""
                    for _ in 1...8 {
                        id += String(B64[B64.index(B64.startIndex, offsetBy: Int(arc4random_uniform(UInt32(B64.characters.count))))])
                    }
                } while (getAll(table: table, column: columns[LocalDatabaseHandler.ID]!).map {
                    $0 ?? ""
                }).contains(id)
                set.append(self.id <- id)
                result = id
            } else {
                result = pairs[1]
            }
            for i in 0..<pairs.count {
                if i % 2 == 0 {
                    set.append(columns[pairs[i]!]! <- pairs[i + 1])
                }
            }
            do {
                try db.run(table.insert(set))
            } catch {
                result = nil
            }
        }
        return result
    }

    @discardableResult
    func inup(table: Table, pairs: [String?]) -> String? {
        return inup(insert: false, _update: false, table: table, pairs: pairs)
    }

    @discardableResult
    func insert(table: Table, pairs: [String?]) -> String? {
        return inup(insert: true, _update: false, table: table, pairs: pairs)
    }

    func update(table: Table, pairs: [String?]) {
        inup(insert: false, _update: true, table: table, pairs: pairs)
    }

    func delete(table: Table, idKey: String, idValue: String) {
        inup(insert: false, _update: false, table: table, pairs: [idKey, idValue, LocalDatabaseHandler.DELETE])
    }

    func truncate(_ table: Table) {
        try! db.run(table.delete())
    }

    static func getTimestamp() -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyyMMddHHmmss"
        return dateFormatter.string(from: Date())
    }

    static func getNiceDate(_ timestamp: String) -> String {
        let thenDF = DateFormatter()
        thenDF.dateFormat = "yyyyMMddHHmmss"
        let then = thenDF.date(from: timestamp)!
        let formatter = DateFormatter()
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .short
        if dateFormatter.string(from: then) == dateFormatter.string(for: Date()) {
            formatter.timeStyle = .short
        } else {
            formatter.dateStyle = .short
        }
        return formatter.string(from: then)
    }

    static func explode(_ input: String?) -> [String] {
        return input != nil ? input!.components(separatedBy: ",") : [""]
    }

    static func implode(_ input: [String]) -> String {
        var result = input[0]
        for x in input[1..<input.count] {
            result += ",\(x)"
        }
        return result
    }

}
