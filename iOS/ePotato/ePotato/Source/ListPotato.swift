import UIKit
import Toast_Swift

public class ListPotato: UITableViewCell {

    var controller: HomeController?
    var table: UITableView?

    var type = 0

    var pid: String = ""
    var uid: String?
    var ts = false
    var uids: [String]?
    var tss: [Bool]?
    var name: String?

    let ldb = LocalDatabaseHandler()

    func setup(_ p: [String: String?], _ type: Int, _ vc: HomeController) {
        controller = vc
        self.type = type
        pid = p[LocalDatabaseHandler.ID]!!
        if type < 2 || LocalDatabaseHandler.explode(p[LocalDatabaseHandler.SENT_POTATOES_UIDS]!).count < 2 {
            uid = type < 2 ? p[LocalDatabaseHandler.UID]!! : LocalDatabaseHandler.explode(p[LocalDatabaseHandler.SENT_POTATOES_UIDS]!)[0]
            uids = [uid!]
            ts = type == 0 && Bool(p[LocalDatabaseHandler.RECEIVED_POTATOES_TS]!!) ?? false
            tss = [ts]
            name = ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: uid!, column: ldb.contactName)
            name = name ?? ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: uid!, column: ldb.epid)
            name = name ?? ldb.get(table: ldb.following, idKey: ldb.uid, idValue: uid!, column: ldb.epid)
            name = name ?? ldb.get(table: ldb.tempContacts, idKey: ldb.uid, idValue: uid!, column: ldb.epid)
            name = name == nil && uid == "-4" ? "Followers" : name
        } else {
            name = ""
            uids = LocalDatabaseHandler.explode(p[LocalDatabaseHandler.SENT_POTATOES_UIDS]!!)
            tss = []
            for uid in uids! {
                tss!.append(false)
                var _name = ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: uid, column: ldb.contactName)
                _name = _name ?? ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: uid, column: ldb.epid)
                _name = _name ?? ldb.get(table: ldb.tempContacts, idKey: ldb.uid, idValue: uid, column: ldb.epid)
                _name = _name == nil && uid == "-4" ? "Followers" : _name
                if let _name = _name {
                    name = name! + name! == "" ? _name : ", " + _name;
                }
                for pname in LocalDatabaseHandler.explode(p[LocalDatabaseHandler.SENT_POTATOES_NAMES]!!) {
                    name = name! + name! == "" ? pname : ", " + pname;
                }
            }
        }
    }

    func action() {
    }

    func reply() {
        let vc = UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "sendPotatoController") as! SendPotatoController
        vc.uids = uids!.filter { s in
            return Int(s)! > 0
        }
        vc.tss = tss!
        controller?.navigationController?.pushViewController(vc, animated: true)
    }

    func addToContacts() {
        let uid: String
        switch type {
        case 0:
            uid = self.uid!
            break
        default:
            uid = self.uids![0]
        }
        if let name = name {
            ldb.inup(table: ldb.contacts, pairs: [LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.EPID, name])
        }
        controller?.refresh()
    }

    func unfollow() {
        ldb.delete(table: ldb.following, idKey: LocalDatabaseHandler.UID, idValue: uid!)
        EndpointsHandler().unfollow(callback: { notice in }, uid: uid!)
        makeToast("Unfollowed!")
    }

    func delete() {
        ldb.delete(table: type == 0 ? ldb.receivedPotatoes : type == 1 ? ldb.feedPotatoes : ldb.sentPotatoes, idKey: LocalDatabaseHandler.ID, idValue: pid)
        if type == 0 {
            ldb.delete(table: ldb.newPotatoes, idKey: LocalDatabaseHandler.NEW_POTATOES_PID, idValue: pid)
        } else if type == 1 {
            ldb.delete(table: ldb.newFeedPotatoes, idKey: LocalDatabaseHandler.NEW_POTATOES_PID, idValue: pid)
        }
        controller?.refresh()
    }
}
