import UIKit

class SendPotatoController: UIViewController {

    @IBOutlet weak var potatoView: PotatoView!
    @IBOutlet weak var navItem: UINavigationItem!

    var uids: [String]?
    var tss: [Bool]?

    static var text = ""
    static var form = 0

    override func viewDidLoad() {
        super.viewDidLoad()
        if let uids = uids, uids.count > 0 {
            let uid = uids[0]
            let ldb = LocalDatabaseHandler()
            var name = ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: uid, column: ldb.contactName)
            name = name ?? ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: uid, column: ldb.epid)
            name = name ?? ldb.get(table: ldb.following, idKey: ldb.uid, idValue: uid, column: ldb.epid)
            name = name ?? ldb.get(table: ldb.tempContacts, idKey: ldb.uid, idValue: uid, column: ldb.epid)
            if let name = name {
                navItem.title = "Potato for \(name + (uids.count > 1 ? ", ..." : ""))"
            }
        }
        potatoView.setup(form: SendPotatoController.form, text: SendPotatoController.text as NSString)
        SendPotatoController.form = potatoView.form
        editText()
    }

    @IBAction func ptap(_ sender: Any) {
        editText()
    }

    @IBAction func change(_ sender: Any) {
        potatoView.form = 0
        SendPotatoController.form = potatoView.form
    }

    @IBAction func ok(_ sender: Any) {
        if let uids = uids, uids.count > 0 {
            let ud = UserDefaults.standard
            var ac: AdController?
            if !(ud.value(forKey: LocalDatabaseHandler.PREMIUM) as? Bool ?? false) {
                ac = UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "adController") as? AdController
                ac!.setup()
            }
            if tss == nil || tss!.count != uids.count {
                tss = []
                for _ in 0..<uids.count {
                    tss!.append(false)
                }
            }
            let ldb = LocalDatabaseHandler()
            let pid = ldb.insert(table: ldb.sentPotatoes, pairs: [
                    LocalDatabaseHandler.DT, LocalDatabaseHandler.getTimestamp(),
                    LocalDatabaseHandler.SENT_POTATOES_UIDS, LocalDatabaseHandler.implode(uids),
                    LocalDatabaseHandler.POTATO_TEXT, SendPotatoController.text,
                    LocalDatabaseHandler.POTATO_FORM, String(SendPotatoController.form)])
            EndpointsHandler().sendPotato(callback: { notice in
                if let sc = SentController.sc {
                    sc.update {
                    }
                }
            }, uids: uids, ts: tss!, message: SendPotatoController.text, type: SendPotatoController.form, pid: pid!)
            let nc = navigationController
            nc?.popToRootViewController(animated: false)
            if let ac = ac {
                nc?.pushViewController(ac, animated: false)
            }
        } else {
            navigationController?.pushViewController(UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "sendPeopleController"), animated: true)
        }
    }

    func editText() {
        let alert = UIAlertController(title: "Enter Message", message: nil, preferredStyle: UIAlertControllerStyle.alert)
        alert.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.default) { action in
            if let tfs = alert.textFields, let t = tfs[0].text {
                SendPotatoController.text = t
                self.potatoView.text = t as NSString
            }
        })
        alert.addTextField { field in
            field.text = SendPotatoController.text
        }
        present(alert, animated: true)
    }

}
