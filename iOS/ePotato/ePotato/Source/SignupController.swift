import UIKit
import Toast_Swift

class SignupController: UIViewController {

    @IBOutlet weak var loading: UIActivityIndicatorView!
    @IBOutlet weak var username: UITextField!
    @IBOutlet weak var password: UITextField!
    @IBOutlet weak var confirmpw: UITextField!
    @IBOutlet weak var notice: UILabel!

    var epid = ""
    var _pw = ""

    override func viewDidLoad() {
        super.viewDidLoad()
        username.text = epid
        password.text = _pw
        confirmpw.text = _pw
    }

    @IBAction func ok(_ sender: Any) {
        epid = username.text ?? ""
        _pw = password.text ?? ""
        let pwc = confirmpw.text ?? ""
        if epid.characters.count == 0 || epid.characters.count == 0 {
            self.view.makeToast("Please enter a username and a password.")
        } else if pwc.characters.count == 0 {
            self.view.makeToast("Please enter your new password into both fields.")
        } else if pwc != _pw {
            self.view.makeToast("The passwords don't match!")
        } else if !epid.matches("^[a-zA-Z0-9._-]{1,30}$") {
            self.view.makeToast("The username can only contain letters, numbers, \'.\', \'-\', \'_\' and must be under 30 characters.")
        } else if _pw.characters.count < 4 {
            self.view.makeToast("The password must be at least 4 characters long!")
        } else {
            loading.isHidden = false
            EndpointsHandler().check(callback: { id in
                self.loading.isHidden = true
                if id == nil {
                    let pc = UIStoryboard(name: "Welcome", bundle: nil).instantiateViewController(withIdentifier: "phone1Controller") as! Phone1Controller
                    pc.epid = self.epid
                    pc._pw = self._pw
                    self.present(pc, animated: true)
                } else {
                    self.view.makeToast("This ePotato-ID is already taken :/")
                }
            }, kind: EndpointsHandler.PROFILE, key: EndpointsHandler.P_EPID, value: epid)
        }
    }

    @IBAction func gotoPW(_ sender: Any) {
        password.becomeFirstResponder()
    }

    @IBAction func gotoCPW(_ sender: Any) {
        confirmpw.becomeFirstResponder()
    }

}
