import UIKit
import Toast_Swift
import StoreKit

class SettingsController: UITableViewController, SKProductsRequestDelegate, SKPaymentTransactionObserver {

    @IBOutlet weak var totalStrangers: UISwitch!
    @IBOutlet weak var epidLabel: UILabel!
    @IBOutlet weak var phoneTitle: UILabel!
    @IBOutlet weak var phoneLabel: UILabel!
    @IBOutlet weak var ratitle: UILabel!
    @IBOutlet weak var removeads: UILabel!

    let ud = UserDefaults.standard

    var premProd: SKProduct?
    var donProd: SKProduct?

    var canpurchase = true

    override func viewDidLoad() {
        super.viewDidLoad()
        epidLabel.text = ud.value(forKey: LocalDatabaseHandler.EPID) as? String
        let phone = ud.value(forKey: LocalDatabaseHandler.PHONE)
        phoneLabel.text = phone as? String ?? "This lets your friends find you when they join ePotato!"
        phoneTitle.text = phone == nil ? "Connect with Phone Number" : "Change Phone Number"
        totalStrangers.setOn(ud.value(forKey: LocalDatabaseHandler.TOTAL_STRANGERS) as! Bool, animated: false)
        if ud.value(forKey: LocalDatabaseHandler.PREMIUM) as? Bool ?? false {
            removeads.text = "Thank you very much for removing the ads 💛"
            ratitle.text = "Donate"
        }
        if SKPaymentQueue.canMakePayments() {
            let productRequest = SKProductsRequest(productIdentifiers: ["premium"/*, "donation"*/])
            productRequest.delegate = self
            productRequest.start()
        } else {
            canpurchase = false
        }
        SKPaymentQueue.default().add(self)
    }

    @IBAction func totalToggle(_ sender: Any? = nil) {
        let state = totalStrangers.isOn
        EndpointsHandler().update(callback: { id in
            if id != nil {
                print(self.ud.value(forKey: LocalDatabaseHandler.TOTAL_STRANGERS) as! Bool)
                self.ud.set(state, forKey: LocalDatabaseHandler.TOTAL_STRANGERS)
                print(self.ud.value(forKey: LocalDatabaseHandler.TOTAL_STRANGERS) as! Bool)
            }
        }, kind: EndpointsHandler.PROFILE, property: EndpointsHandler.ID, value: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int), properties: [EndpointsHandler.P_STRANGERS], values: [String(state)])
    }

    public override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        switch (indexPath.section * 100 + indexPath.row) {
        case 0:
            totalStrangers.setOn(!totalStrangers.isOn, animated: true)
            totalToggle()
            break
        case 100:
            let alert = UIAlertController(title: "Change ePotato-ID", message: "Enter a new username.", preferredStyle: UIAlertControllerStyle.alert)
            alert.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.default) { action in
                if let tfs = alert.textFields, let epid = tfs[0].text, let pw = tfs[1].text {
                    if epid.characters.count == 0 || pw.characters.count == 0 {
                        self.display("Please enter a username and your password.", again: {
                            self.tableView(tableView, didSelectRowAt: indexPath)
                        })
                    } else if !epid.matches("^[a-zA-Z0-9._-]{1,30}$") {
                        self.display("The username can only contain letters, numbers, \'.\', \'-\', \'_\' and must be under 30 characters.", again: {
                            self.tableView(tableView, didSelectRowAt: indexPath)
                        })
                    } else {
                        EndpointsHandler().check(callback: { id in
                            if id != nil {
                                self.display("This ePotato-ID is already taken :/", again: {
                                    self.tableView(tableView, didSelectRowAt: indexPath)
                                })
                            } else {
                                EndpointsHandler().login(callback: { notice in
                                    if let n = notice {
                                        if n.ok!.boolValue {
                                            EndpointsHandler().update(callback: { id in
                                                AppDelegate.updateFIEPID()
                                                self.epidLabel.text = epid
                                            }, kind: EndpointsHandler.PROFILE, property: EndpointsHandler.ID, value: String(self.ud.value(forKey: LocalDatabaseHandler.UID) as! Int), properties: [EndpointsHandler.P_EPID], values: [epid])
                                        } else {
                                            self.display("Wrong Password :(", again: {
                                                self.tableView(tableView, didSelectRowAt: indexPath)
                                            })
                                        }
                                    } else {
                                        self.display("Error :( Please try logging out and in again.", again: {
                                            self.tableView(tableView, didSelectRowAt: indexPath)
                                        })
                                    }
                                }, epid: epid, password: pw.sha256())
                            }
                        }, kind: EndpointsHandler.PROFILE, key: EndpointsHandler.P_EPID, value: epid)
                    }
                }
            })
            alert.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.cancel))
            alert.addTextField { field in
                field.text = self.ud.value(forKey: LocalDatabaseHandler.EPID) as? String
            }
            alert.addTextField { field in
                field.isSecureTextEntry = true
                field.placeholder = "Password"
            }
            present(alert, animated: true)
            break
        case 101:
            let vc = UIStoryboard(name: "Welcome", bundle: nil).instantiateViewController(withIdentifier: "phone1Controller") as! Phone1Controller
            vc.number = ud.value(forKey: LocalDatabaseHandler.PHONE) as? String ?? ""
            vc.phoneChange = true
            present(vc, animated: true, completion: nil)
            break
        case 102:
            let alert = UIAlertController(title: "Change Password", message: nil, preferredStyle: UIAlertControllerStyle.alert)
            alert.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.default) { action in
                if let tfs = alert.textFields, let opw = tfs[0].text, let npw = tfs[1].text, let cpw = tfs[2].text {
                    if opw.characters.count == 0 || npw.characters.count == 0 || cpw.characters.count == 0 {
                        self.display("Please enter your current and new password into the fields.", again: {
                            self.tableView(tableView, didSelectRowAt: indexPath)
                        })
                    } else if npw != cpw {
                        self.display("The passwords don't match!", again: {
                            self.tableView(tableView, didSelectRowAt: indexPath)
                        })
                    } else if npw.characters.count < 4 {
                        self.display("The password must be at least 4 characters long!", again: {
                            self.tableView(tableView, didSelectRowAt: indexPath)
                        })
                    } else {
                        EndpointsHandler().changePassword(callback: { notice in
                            if let n = notice {
                                if n.ok!.boolValue {
                                    self.ud.set(npw.sha256(), forKey: LocalDatabaseHandler.PASSWORD)
                                } else {
                                    self.display("Wrong Password :(", again: {
                                        self.tableView(tableView, didSelectRowAt: indexPath)
                                    })
                                }
                            } else {
                                self.display("An error has occured! Sorry :(", again: {
                                    self.tableView(tableView, didSelectRowAt: indexPath)
                                })
                            }
                        }, epid: String(self.ud.value(forKey: LocalDatabaseHandler.UID) as! Int), npw: npw.sha256(), opw: opw.sha256())
                    }
                }
            })
            alert.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.cancel))
            alert.addTextField { field in
                field.isSecureTextEntry = true
                field.placeholder = "Old Password"
            }
            alert.addTextField { field in
                field.isSecureTextEntry = true
                field.placeholder = "New Password"
            }
            alert.addTextField { field in
                field.isSecureTextEntry = true
                field.placeholder = "Confirm New Password"
            }
            present(alert, animated: true)
            break
        case 103:
            let alert = UIAlertController(title: "Log Out", message: "Do you want to log out? Everything will be saved.", preferredStyle: UIAlertControllerStyle.alert)
            alert.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.default) { action in
                AppDelegate.logout(self)
            })
            alert.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.cancel))
            present(alert, animated: true)
            break
        case 104:
            let alert = UIAlertController(title: "Delete Account", message: "Do you really want to delete your account? This cannot be undone!", preferredStyle: UIAlertControllerStyle.alert)
            alert.addAction(UIAlertAction(title: "Delete", style: UIAlertActionStyle.destructive) { action in
                if let tfs = alert.textFields, let pw = tfs[0].text {
                    if pw.characters.count == 0 {
                        self.display("Please enter your password.", again: {
                            self.tableView(tableView, didSelectRowAt: indexPath)
                        })
                    } else {
                        EndpointsHandler().deleteAccount(callback: { notice in
                            if let n = notice {
                                if n.ok!.boolValue {
                                    AppDelegate.logout(self)
                                } else {
                                    self.display("Wrong Password :(", again: {
                                        self.tableView(tableView, didSelectRowAt: indexPath)
                                    })
                                }
                            } else {
                                self.display("An error has occured! Sorry :(", again: {
                                    self.tableView(tableView, didSelectRowAt: indexPath)
                                })
                            }
                        }, epid: String(self.ud.value(forKey: LocalDatabaseHandler.UID) as! Int), pw: pw)
                    }
                    AppDelegate.logout(self)
                }
            })
            alert.addAction(UIAlertAction(title: "Cancel", style: UIAlertActionStyle.cancel))
            alert.addTextField { field in
                field.isSecureTextEntry = true
                field.placeholder = "Password"
            }
            present(alert, animated: true)
            break
        case 200:
            if let url = URL(string: "itms-apps://itunes.apple.com/app/id1287216889"), UIApplication.shared.canOpenURL(url) {
                if #available(iOS 10.0, *) {
                    UIApplication.shared.open(url, options: [:], completionHandler: nil)
                } else {
                    UIApplication.shared.openURL(url)
                }
            }
            break
        case 201:
            SKPaymentQueue.default().restoreCompletedTransactions()
            if canpurchase {
                if ud.value(forKey: LocalDatabaseHandler.PREMIUM) as? Bool ?? false, let p = donProd {
                    SKPaymentQueue.default().add(SKPayment(product: p))
                } else if let p = premProd {
                    SKPaymentQueue.default().add(SKPayment(product: p))
                }
            }
            break
        case 202:
            SKPaymentQueue.default().restoreCompletedTransactions()
            break
        default:
            let alert = UIAlertController(title: nil, message: "ePotato is created by Onno Eberhard :)", preferredStyle: UIAlertControllerStyle.alert)
            alert.addAction(UIAlertAction(title: "Website", style: UIAlertActionStyle.default) { action in
                if let url = URL(string: "https://onnoeberhard.com/ePotato"), UIApplication.shared.canOpenURL(url) {
                    if #available(iOS 10.0, *) {
                        UIApplication.shared.open(url, options: [:], completionHandler: nil)
                    } else {
                        UIApplication.shared.openURL(url)
                    }
                }
            })
            alert.addAction(UIAlertAction(title: "Licenses", style: UIAlertActionStyle.default) { action in
                if let url = URL(string: "https://onnoeberhard.com/ePotato/ios-licenses"), UIApplication.shared.canOpenURL(url) {
                    if #available(iOS 10.0, *) {
                        UIApplication.shared.open(url, options: [:], completionHandler: nil)
                    } else {
                        UIApplication.shared.openURL(url)
                    }
                }
            })
            alert.addAction(UIAlertAction(title: "OK", style: UIAlertActionStyle.default))
            present(alert, animated: true)
            break
        }
    }

    override func numberOfSections(in tableView: UITableView) -> Int {
        return 3
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return section == 0 ? 1 : section == 1 ? 5 : 4;
    }

    public func productsRequest(_ request: SKProductsRequest, didReceive response: SKProductsResponse) {
        for p in response.products {
            if p.productIdentifier == "premium" {
                premProd = p
            } else if p.productIdentifier == "donation" {
                donProd = p
            }
        }
    }

    public func paymentQueue(_ queue: SKPaymentQueue, updatedTransactions transactions: [SKPaymentTransaction]) {
        for t in transactions {
            if t.transactionState == SKPaymentTransactionState.purchased || t.transactionState == SKPaymentTransactionState.restored {
                ud.set(true, forKey: LocalDatabaseHandler.PREMIUM)
                ud.synchronize()
                removeads.text = "Thank you very much for removing the ads 💛"
                ratitle.text = "Donate"
            }
            if t.transactionState != SKPaymentTransactionState.purchasing {
                SKPaymentQueue.default().finishTransaction(t)
            }
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        SKPaymentQueue.default().remove(self)
    }

}
