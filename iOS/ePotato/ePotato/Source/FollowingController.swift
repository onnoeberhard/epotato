import UIKit

class FollowingController: CFController {

    static var fc: FollowingController?

    var items = [Section: [[String: String?]]]()

    enum Section: Int {
        case following = 0, suggestions, total
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        FollowingController.fc = self
        update()
    }

    func addperson() {
        let alert = UIAlertController(title: "Follow Someone", message: nil, preferredStyle: UIAlertControllerStyle.alert)
        alert.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.default) { action in
            if let tfs = alert.textFields, let t = tfs[0].text {
                self.showLoading {
                    EndpointsHandler().getProfile(callback: { profile in
                        self.hideLoading()
                        let ldb = LocalDatabaseHandler()
                        if let p = profile, ldb.get(table: ldb.contacts, idKey: ldb.uid, idValue: p.identifier!.stringValue, column: ldb.id) == nil && p.identifier!.intValue != UserDefaults.standard.value(forKey: LocalDatabaseHandler.UID) as! Int {
                            ldb.inup(table: ldb.following, pairs: [LocalDatabaseHandler.UID, p.identifier!.stringValue, LocalDatabaseHandler.EPID, p.epid])
                            EndpointsHandler().follow(callback: { notice in }, uid: p.identifier!.stringValue)
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
        let contacts = ldb.getAll(table: ldb.contacts)
        items[.following] = ldb.getAll(table: ldb.following).filter({
            let x = $0[LocalDatabaseHandler.UID]!!;
            return !contacts.contains(where: { $0[LocalDatabaseHandler.UID]!! == x })
        })
        items[.suggestions] = ldb.getAll(table: ldb.suggestedFollowing)
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
                self.update()
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

    override func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        if let s = Section(rawValue: section) {
            switch s {
            case .following:
                return "Following"
            case .suggestions:
                return "Who to Follow"
            default:
                return nil
            }
        }
        return nil
    }

    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return CGFloat(56)
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let section = Section(rawValue: indexPath.section)!
        let cell = tableView.dequeueReusableCell(withIdentifier: "contactSingle", for: indexPath) as! ContactSingle
        cell.vc = self
        cell.uid = items[section]![indexPath.row][LocalDatabaseHandler.UID]!
        cell.name_ = items[section]![indexPath.row][LocalDatabaseHandler.EPID]!
        cell.epid = cell.name_
        cell.type = section == .following ? 1 : 2
        cell.setup()
        return cell
    }

    public override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        (tableView.cellForRow(at: indexPath) as! ContactCell).action()
        tableView.deselectRow(at: indexPath, animated: true)
    }


}
