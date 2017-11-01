import UIKit
import SelectionDialog

class ListPotatoSmall: ListPotato {

    @IBOutlet weak var date: UILabel!
    @IBOutlet weak var potato: PotatoView!
    @IBOutlet weak var label: UILabel!

    override func setup(_ p: [String: String?], _ type: Int, _ vc: HomeController) {
        super.setup(p, type, vc)
        label.text = name ?? ""
        date.text = (type == 0 && (p[LocalDatabaseHandler.RECEIVED_POTATOES_TS]!! as NSString).boolValue ? "Total Stranger!\n" : "") + LocalDatabaseHandler.getNiceDate(p[LocalDatabaseHandler.DT]!!)
        potato.setup(form: Int(p[LocalDatabaseHandler.POTATO_FORM]!!)!, text: p[LocalDatabaseHandler.POTATO_TEXT]!! as NSString)
    }

    override func action() {
        let dlg = SelectionDialog(title: "Potato\(name != nil ? (type == 2 ? " for \(name!)" : " from \(name!)") : "")", closeButtonTitle: "Close")
        switch type {
        case 0:
            let addToContacts = ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: uid!, column: ldb.id) == nil && name != nil
            dlg.addItem(item: "Reply!") {
                self.reply()
                dlg.close();
            }
            if addToContacts {
                dlg.addItem(item: "Add \(name!) to Contacts") {
                    self.addToContacts()
                    dlg.close();
                }
            }
            dlg.addItem(item: "Delete") {
                self.delete()
                dlg.close();
            }
            break
        case 1:
            let following = ldb.get(table: ldb.following, idKey: ldb.uid, idValue: uid!, column: ldb.id) != nil && ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: uid!, column: ldb.id) == nil
            dlg.addItem(item: "Reply!") {
                self.reply()
                dlg.close();
            }
            if following {
                dlg.addItem(item: "Unfollow \(name!)") {
                    self.unfollow()
                    dlg.close();
                }
            }
            dlg.addItem(item: "Delete") {
                self.delete()
                dlg.close();
            }
            break
        default:
            let addToContacts = uid != nil && ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: uid!, column: ldb.id) == nil && name != nil
            if addToContacts {
                dlg.addItem(item: "Add \(name!) to Contacts") {
                    self.addToContacts()
                    dlg.close();
                }
            }
            dlg.addItem(item: "Send new Potato") {
                self.reply()
                dlg.close();
            }
            dlg.addItem(item: "Delete") {
                self.delete()
                dlg.close();
            }
            break
        }
        dlg.show()
    }

    override func awakeFromNib() {
        super.awakeFromNib()
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)
    }

}
