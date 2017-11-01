import UIKit
import SelectionDialog

class ContactCell: UITableViewCell {

    var vc: CFController?

    var type = 0
    var uid: String?
    var name_: String?
    var epid: String?

    func action() {
        let ldb = LocalDatabaseHandler()
        switch type {
        case 0:
            let dlg = SelectionDialog(title: name_!, closeButtonTitle: "Close")
            dlg.addItem(item: "Send a Potato!") {
                self.send()
                dlg.close();
            }
            if ldb.get(table: ldb.phoneContacts, idKey: ldb.uid, idValue: uid!, column: ldb.contactPhone) == nil {
                dlg.addItem(item: "Remove Contact") {
                    self.removeContact()
                    dlg.close();
                }
            }
            dlg.show()
        case 1:
            let dlg = SelectionDialog(title: name_!, closeButtonTitle: "Close")
            dlg.addItem(item: "Unfollow") {
                self.unfollow()
                dlg.close();
            }
            dlg.show()
        case 2:
            let dlg = UIAlertController(title: "Follow \(name_!)", message: nil, preferredStyle: UIAlertControllerStyle.alert)
            dlg.addAction(UIAlertAction(title: "Follow", style: UIAlertActionStyle.default) { action in
                self.follow()
            })
            dlg.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.cancel))
            vc?.present(dlg, animated: true)
        case 3:
            let dlg = UIAlertController(title: nil, message: "\(name_!) doesn't have ePotato yet! Consider showing them the app :)", preferredStyle: UIAlertControllerStyle.alert)
            dlg.addAction(UIAlertAction(title: "Ok", style: UIAlertActionStyle.default))
            vc?.present(dlg, animated: true)
        default:
            break
        }
    }

    func send() {
        let vc = UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "sendPotatoController") as! SendPotatoController
        vc.uids = [uid!]
        vc.tss = [false]
        self.vc?.navigationController?.pushViewController(vc, animated: true)
    }

    func removeContact() {
        let dlg = UIAlertController(title: "Remove \(name_!)?", message: nil, preferredStyle: UIAlertControllerStyle.alert)
        dlg.addAction(UIAlertAction(title: "Remove", style: UIAlertActionStyle.default) { action in
            EndpointsHandler().deleteContact(callback: { notice in }, uid: self.uid!)
            let ldb = LocalDatabaseHandler()
            ldb.inup(table: ldb.tempContacts, pairs: [LocalDatabaseHandler.UID, self.uid!, LocalDatabaseHandler.EPID, self.epid!])
            ldb.delete(table: ldb.contacts, idKey: LocalDatabaseHandler.UID, idValue: self.uid!)
            self.vc?.update()
        })
        dlg.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.cancel))
        vc?.present(dlg, animated: true)
    }

    func unfollow() {
        let dlg = UIAlertController(title: "Unfollow \(name_!)?", message: nil, preferredStyle: UIAlertControllerStyle.alert)
        dlg.addAction(UIAlertAction(title: "Unfollow", style: UIAlertActionStyle.default) { action in
            EndpointsHandler().unfollow(callback: { notice in }, uid: self.uid!)
            let ldb = LocalDatabaseHandler()
            ldb.inup(table: ldb.tempContacts, pairs: [LocalDatabaseHandler.UID, self.uid!, LocalDatabaseHandler.EPID, self.epid!])
            ldb.delete(table: ldb.following, idKey: LocalDatabaseHandler.UID, idValue: self.uid!)
            self.vc?.update()
        })
        dlg.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.cancel))
        vc?.present(dlg, animated: true)
    }

    func follow() {
        let ldb = LocalDatabaseHandler()
        ldb.inup(table: ldb.following, pairs: [LocalDatabaseHandler.UID, uid!, LocalDatabaseHandler.EPID, epid])
        ldb.delete(table: ldb.suggestedFollowing, idKey: LocalDatabaseHandler.UID, idValue: uid!)
        EndpointsHandler().follow(callback: { notice in }, uid: uid!)
        self.vc?.update()
    }

}
