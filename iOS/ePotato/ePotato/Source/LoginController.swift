import UIKit
import Toast_Swift

class LoginController: UIViewController {

    @IBOutlet weak var loading: UIActivityIndicatorView!
    @IBOutlet weak var epidTF: UITextField!
    @IBOutlet weak var pwTF: UITextField!
    @IBOutlet weak var recoverLabel: UILabel!
    @IBOutlet weak var forgotPassword: UIButton!

    var epid = ""
    var recoveredPassword = false

    override func viewDidLoad() {
        super.viewDidLoad()
        epidTF.text = epid
        if recoveredPassword {
            recoverLabel.isHidden = false
            forgotPassword.setTitle("Send SMS again".localized, for: .normal)
        }
    }

    @IBAction func login(_ sender: Any) {
        epid = epidTF.text ?? ""
        let _pw = pwTF.text ?? ""
        if epid.characters.count == 0 || _pw.characters.count == 0 {
            self.view.makeToast("Please enter your ePotato-ID / phone number and password.")
        } else {
            let pw = _pw.sha256()
            loading.isHidden = false
            EndpointsHandler().login(callback: { notice in
                self.loading.isHidden = true
                if let notice = notice, notice.code! != EndpointsHandler.NOTICE_NULL {
                    if notice.code! == EndpointsHandler.NOTICE_ERROR {
                        self.view.makeToast("Wrong Password :(")
                    } else if notice.code! == EndpointsHandler.NOTICE_OTHER {
                        let vc = UIStoryboard(name: "Welcome", bundle: nil).instantiateViewController(withIdentifier: "recoverPasswordController") as! RecoverPasswordController
                        vc.id = NSNumber(value: Int(notice.message ?? "0")!)
                        vc.epid = self.epid
                        vc.opw = pw
                        self.present(vc, animated: false, completion: nil)
                    } else {
                        self.loading.isHidden = false
                        EndpointsHandler().getProfile(callback: { profile in
                            self.loading.isHidden = true
                            if let profile = profile {
                                let ud = UserDefaults.standard
                                ud.set(profile.identifier, forKey: LocalDatabaseHandler.UID)
                                ud.set(pw, forKey: LocalDatabaseHandler.PASSWORD)
                                ud.set(profile.epid, forKey: LocalDatabaseHandler.EPID)
                                ud.set(profile.phone, forKey: LocalDatabaseHandler.PHONE)
                                ud.synchronize()
                                self.present(UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "homeController") as UIViewController, animated: false, completion: nil)
                            } else {
                                self.view.makeToast("Wrong Password :(")
                            }
                        }, key: EndpointsHandler.ID, value: notice.message!)
                    }
                } else {
                    self.view.makeToast("There is no account with that ePotato-ID / phone number.")
                }
            }, epid: epid, password: pw)
        }
    }

    @IBAction func forgot(_ sender: Any) {
        epid = epidTF.text ?? ""
        if epid.characters.count == 0 {
            self.view.makeToast("Please enter your ePotato-ID or phone number.")
        } else {
            loading.isHidden = false
            EndpointsHandler().recoverPassword(callback: { notice in
                self.loading.isHidden = true
                if let notice = notice {
                    if !notice.ok!.boolValue {
                        self.view.makeToast("You can't recover your password, because you didn't set a phone number. We're sorry :(")
                    } else {
                        self.view.makeToast("We have sent you a new password via SMS.")
                        self.recoveredPassword = true
                        self.viewDidLoad()
                    }
                } else {
                    self.view.makeToast("There is no account with that ePotato-ID / phone number.")
                }
            }, epid: epid)
        }
    }

    @IBAction func gotoPW(_ sender: Any) {
        pwTF.becomeFirstResponder()
    }
}
