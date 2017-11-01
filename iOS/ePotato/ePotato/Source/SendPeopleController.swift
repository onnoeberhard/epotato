import UIKit

class SendPeopleController: UIViewController, UITableViewDataSource, UITableViewDelegate {

    @IBOutlet weak var tableView: UITableView!

    var items = [Section: [[String: String?]]]()
    var cells = [IndexPath: PeoplePerson]()
    var selected = [IndexPath: Bool]()

    enum Section: Int {
        case top = 0, contacts, phone, total
    }

    var uids = [String]()
    var tss = [Bool]()
    var ts = false
    var phones = false

    var ac: AdController?

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.delegate = self
        tableView.dataSource = self
        update()
        let ud = UserDefaults.standard
        if !(ud.value(forKey: LocalDatabaseHandler.PREMIUM) as? Bool ?? false) {
            ac = UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "adController") as? AdController
            ac?.setup()
        }
    }

    @IBAction func addperson(_ sender: Any) {
        let alert = UIAlertController(title: "Add Contact", message: nil, preferredStyle: UIAlertControllerStyle.alert)
        alert.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.default) { action in
            if let tfs = alert.textFields, let t = tfs[0].text {
                self.showLoading {
                    EndpointsHandler().getProfile(callback: { profile in
                        self.hideLoading()
                        let ldb = LocalDatabaseHandler()
                        if let p = profile, ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: p.identifier!.stringValue, column: ldb.id) == nil && p.identifier!.intValue != UserDefaults.standard.value(forKey: LocalDatabaseHandler.UID) as! Int {
                            ldb.delete(table: ldb.following, idKey: LocalDatabaseHandler.UID, idValue: p.identifier!.stringValue)
                            ldb.inup(table: ldb.contacts, pairs: [LocalDatabaseHandler.UID, p.identifier!.stringValue, LocalDatabaseHandler.EPID, p.epid])
                            EndpointsHandler().newContact(callback: { notice in }, uid: p.identifier!.stringValue)
                            self.update()
                        } else {
                            self.display("No match found!", again: {
                                self.addperson(sender)
                            })
                        }
                    }, key: EndpointsHandler.P_EPID, value: t)
                }
            }
        })
        alert.addTextField(configurationHandler: { tf in
            tf.placeholder = "Enter ePotato-ID"
        })
        present(alert, animated: true)
    }

    @IBAction func reload(_ sender: Any) {
        showLoading {
            AppDelegate.loadContacts(self, {
                self.update(false)
                self.hideLoading()
            })
        }
    }

    var alert: UIAlertController?

    func showLoading(_ visible: (() -> Void)? = nil) {
        alert = UIAlertController(title: nil, message: "Loading Contacts...", preferredStyle: .alert)
        let loadingIndicator = UIActivityIndicatorView(frame: CGRect(x: 10, y: 5, width: 50, height: 50))
        loadingIndicator.hidesWhenStopped = true
        loadingIndicator.activityIndicatorViewStyle = UIActivityIndicatorViewStyle.gray
        loadingIndicator.startAnimating();
        alert!.view.addSubview(loadingIndicator)
        present(alert!, animated: true, completion: visible)
    }

    func hideLoading() {
        alert?.dismiss(animated: true, completion: nil)
        alert = nil
    }

    func update(_ load: Bool = true) {
        let ldb = LocalDatabaseHandler()
        if ldb.getAll(table: ldb.contacts, column: ldb.id).count == 0 && load {
            reload(0)
        } else {
            items[.top] = [[:], [LocalDatabaseHandler.EPID: "Post to Feed!", LocalDatabaseHandler.UID: "-4"], [LocalDatabaseHandler.EPID: "Send to a Total Stranger!", LocalDatabaseHandler.UID: "-2"]]
            items[.contacts] = ldb.getAll(table: ldb.contacts)
            items[.phone] = ldb.getAll(table: ldb.phoneContacts)
        }
        let group = DispatchGroup()
        group.enter()
        DispatchQueue.main.async {
            self.tableView.reloadData()
            group.leave()
        }
    }

    @IBAction func sendPotato(_ sender: Any) {
        uids = []
        ts = false
        phones = false
        for c in cells.values {
            if let i = c.item, c.box.isOn && !i.keys.contains(where: { $0 == LocalDatabaseHandler.UID }) {
                self.phones = true
            } else if let i = c.item, c.box.isOn && i[LocalDatabaseHandler.UID]!! == "-2" {
                self.ts = true
            } else if let i = c.item, c.box.isOn && !uids.contains(i[LocalDatabaseHandler.UID]!!) {
                uids.append(i[LocalDatabaseHandler.UID]!!)
                tss.append(false)
            }
        }
        if !ts && !phones && uids.count == 0 {
            display("You did not select any receivers!", completed: {
                self.sendOn()
            })
        } else if phones {
            display("Some of the contacts you selected don't have ePotato yet!\nConsider showing them the app :)", completed: {
                self.sendOn()
            })
        } else {
            sendOn()
        }
    }

    func sendOn() {
        if ts {
            EndpointsHandler().getTS(callback: { collection in
                if let ps = collection?.items {
                    self.uids.append(ps[0].identifier!.stringValue)
                    self.tss.append(true)
                    let ldb = LocalDatabaseHandler()
                    ldb.inup(table: ldb.tempContacts, pairs: [LocalDatabaseHandler.UID, ps[0].identifier!.stringValue, LocalDatabaseHandler.EPID, ps[0].epid])
                }
                self.sendOnOn()
            }, n: 1)
        } else if uids.count > 0 {
            sendOnOn()
        }
        finish()
    }

    func sendOnOn() {
        let ldb = LocalDatabaseHandler()
        let pid = ldb.insert(table: ldb.sentPotatoes, pairs: [
                LocalDatabaseHandler.DT, LocalDatabaseHandler.getTimestamp(),
                LocalDatabaseHandler.SENT_POTATOES_UIDS, uids.count > 0 ? LocalDatabaseHandler.implode(uids) : nil,
                LocalDatabaseHandler.POTATO_TEXT, SendPotatoController.text,
                LocalDatabaseHandler.POTATO_FORM, String(SendPotatoController.form)])
        EndpointsHandler().sendPotato(callback: { notice in
            if let sc = SentController.sc {
                sc.update {
                }
            }
        }, uids: uids, ts: tss, message: SendPotatoController.text, type: SendPotatoController.form, pid: pid!)
        finish()
    }

    func finish() {
        let nc = navigationController
        nc?.popToRootViewController(animated: false)
        if let ac = ac {
            nc?.pushViewController(ac, animated: false)
        }
    }

    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if let cell = tableView.cellForRow(at: indexPath), cell.isKind(of: PeoplePerson.self) {
            let pp = tableView.cellForRow(at: indexPath) as! PeoplePerson
            pp.box.setOn(!pp.box.isOn, animated: true)
            pp.onchange(0)
        }
        tableView.deselectRow(at: indexPath, animated: true)
    }

    public func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let section = Section(rawValue: indexPath.section)!
        if section == .top && indexPath.row == 0 {
            let cell = tableView.dequeueReusableCell(withIdentifier: "peoplePotato") as! PeoplePotato
            cell.pv.setup(form: SendPotatoController.form, text: SendPotatoController.text as NSString)
            return cell
        } else {
            let cell = tableView.dequeueReusableCell(withIdentifier: "peoplePerson", for: indexPath) as! PeoplePerson
            cell.indexPath = indexPath
            cell.spc = self
            cell.item = items[section]![indexPath.row]
            var name = items[section]![indexPath.row][LocalDatabaseHandler.EPID]
            if let n = items[section]![indexPath.row][LocalDatabaseHandler.CONTACT_NAME] ?? nil {
                name = n
            }
            cell.label.text = name ?? ""
            cell.box.setOn(selected[indexPath] ?? false, animated: false)
            cells[indexPath] = cell
            return cell
        }
    }

    public func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        if let s = Section(rawValue: indexPath.section), s == .top && indexPath.row == 0 {
            return CGFloat(208)
        }
        return CGFloat(56)
    }

    public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if let s = Section(rawValue: section), let items = items[s] {
            return items.count
        }
        return 0
    }

    public func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if let s = Section(rawValue: section) {
            switch s {
            case .contacts:
                return "Contacts"
            case .phone:
                return "Phone Contacts"
            default:
                return nil
            }
        }
        return nil
    }


    public func numberOfSections(in tableView: UITableView) -> Int {
        return Section.total.rawValue
    }

}
