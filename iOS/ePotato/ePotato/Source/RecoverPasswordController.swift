import UIKit
import Toast_Swift

class RecoverPasswordController: UIViewController {

    var id: NSNumber = 0
    var epid = ""
    var opw = ""

    @IBOutlet weak var pwTF: UITextField!
    @IBOutlet weak var confirmTF: UITextField!
    @IBOutlet weak var loading: UIActivityIndicatorView!

    override func viewDidLoad() {
        super.viewDidLoad()
    }

    @IBAction func ok(_ sender: Any) {
        let pw1 = pwTF.text ?? ""
        let pw2 = confirmTF.text ?? ""
        if pw1.characters.count == 0 || pw2.characters.count == 0 {
            self.view.makeToast("Please enter your new password into both fields.")
        } else if pw1 != pw2 {
            self.view.makeToast("The passwords don\'t match!")
        } else if pw1.characters.count < 4 {
            self.view.makeToast("The password must be at least 4 characters long!")
        } else {
            loading.isHidden = false
            let npw = pw1.sha256()
            EndpointsHandler().changePassword(callback: { notice in
                self.loading.isHidden = true
                if let notice = notice, notice.ok!.boolValue {
                    self.loading.isHidden = false
                    EndpointsHandler().getProfile(callback: { profile in
                        self.loading.isHidden = true
                        if let profile = profile {
                            let ud = UserDefaults.standard
                            ud.set(profile.identifier, forKey: LocalDatabaseHandler.UID)
                            ud.set(npw, forKey: LocalDatabaseHandler.PASSWORD)
                            ud.set(profile.epid, forKey: LocalDatabaseHandler.EPID)
                            ud.set(profile.phone, forKey: LocalDatabaseHandler.PHONE)
                            ud.synchronize()
                            self.present(UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "homeController"), animated: false, completion: nil)
                        } else {
                            self.view.makeToast("An error has occurred! Sorry :(")
                        }
                    }, key: EndpointsHandler.ID, value: self.id.stringValue)
                } else {
                    self.view.makeToast("An error has occurred! Sorry :(")
                }
            }, epid: epid, npw: npw, opw: opw)
        }
    }

    @IBAction func gotoCPW(_ sender: Any) {
        confirmTF.becomeFirstResponder()
    }

    @IBAction func back(_ sender: Any) {
        let loginController: LoginController = UIStoryboard(name: "Welcome", bundle: nil).instantiateViewController(withIdentifier: "loginController") as! LoginController
        loginController.epid = epid
        self.present(loginController, animated: false, completion: nil)
    }
}
