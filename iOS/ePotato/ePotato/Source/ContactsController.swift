import UIKit

class ContactsController: CFController {

    static var cc: ContactsController?

    var items = [Section: [[String: String?]]]()

    enum Section: Int {
        case contacts = 0, phone, total
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        ContactsController.cc = self
        update()
    }

    func addperson() {
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
                        } else if let p = profile, ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: p.identifier!.stringValue, column: ldb.id) != nil {
                            self.display("Already a contact!", again: {
                                self.addperson()
                            })
                        } else {
                            self.display("No match found!", again: {
                                self.addperson()
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

    override func update(_ load: Bool = true) {
        let ldb = LocalDatabaseHandler()
        if ldb.getAll(table: ldb.contacts, column: ldb.id).count == 0 && load {
            reload()
        } else {
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

    func reload() {
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

    override func numberOfSections(in tableView: UITableView) -> Int {
        return Section.total.rawValue
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if let s = Section(rawValue: section), let items = items[s] {
            return items.count
        }
        return 0
    }

    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        if tableView.cellForRow(at: indexPath)?.isKind(of: ContactDouble.self) ?? false {
            return CGFloat(64)
        }
        return CGFloat(56)
    }

    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
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

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let section = Section(rawValue: indexPath.section)!
        let name = items[section]![indexPath.row][LocalDatabaseHandler.CONTACT_NAME]
        let epid = items[section]![indexPath.row][LocalDatabaseHandler.EPID]
        if let epid = epid ?? nil {
            if let name = name ?? nil {
                let cell = tableView.dequeueReusableCell(withIdentifier: "contactDouble", for: indexPath) as! ContactDouble
                cell.vc = self
                cell.uid = items[section]![indexPath.row][LocalDatabaseHandler.UID]!
                cell.epid = epid
                cell.name_ = name
                cell.type = 0
                cell.setup()
                return cell
            } else {
                let cell = tableView.dequeueReusableCell(withIdentifier: "contactSingle", for: indexPath) as! ContactSingle
                cell.vc = self
                cell.uid = items[section]![indexPath.row][LocalDatabaseHandler.UID]!
                cell.name_ = epid
                cell.epid = epid
                cell.type = 0
                cell.setup()
                return cell
            }
        } else if let name = name ?? nil {
            let cell = tableView.dequeueReusableCell(withIdentifier: "contactSingle", for: indexPath) as! ContactSingle
            cell.vc = self
            cell.name_ = name
            cell.type = 3
            cell.setup()
            return cell
        }
        return tableView.dequeueReusableCell(withIdentifier: "contactSingle", for: indexPath)
    }

    public override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        (tableView.cellForRow(at: indexPath) as! ContactCell).action()
        tableView.deselectRow(at: indexPath, animated: true)
    }


}
