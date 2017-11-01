import UIKit
import SelectionDialog

class ListPotatoBig: ListPotato {

    @IBOutlet weak var label: UILabel!
    @IBOutlet weak var more: UIButton!
    @IBOutlet weak var date: UILabel!
    @IBOutlet weak var potato: PotatoView!

    override func setup(_ p: [String: String?], _ type: Int, _ vc: HomeController) {
        super.setup(p, type, vc)
        label.text = name ?? ""
        date.text = ((p[LocalDatabaseHandler.RECEIVED_POTATOES_TS]!! as NSString).boolValue ? "Total Strangers!\n" : "") + LocalDatabaseHandler.getNiceDate(p[LocalDatabaseHandler.DT]!!)
        potato.setup(form: Int(p[LocalDatabaseHandler.POTATO_FORM]!!)!, text: p[LocalDatabaseHandler.POTATO_TEXT]!! as NSString)
    }

    override func action() {
        reply()
    }

    @IBAction func doMore(_ sender: Any) {
        let dlg = SelectionDialog(title: "Potato\(name != nil ? (type == 2 ? " for \(name!)" : " from \(name!)") : "")", closeButtonTitle: "Close")
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
        dlg.show()
    }
}
