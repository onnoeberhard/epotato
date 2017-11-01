import UIKit
import PhoneNumberKit
import Toast_Swift

class Phone1Controller: UIViewController {

    @IBOutlet weak var phone: PhoneNumberTextField!
    @IBOutlet weak var loading: UIActivityIndicatorView!
    @IBOutlet weak var backButton: UIButton!
    @IBOutlet weak var skipButton: UIButton!

    var phoneChange = false

    var epid = ""
    var _pw = ""
    var number = ""

    override func viewDidLoad() {
        super.viewDidLoad()
        phone.text = number
        if phoneChange {
            backButton.setTitle("Cancel", for: .normal)
            skipButton.isHidden = true
        }
    }

    @IBAction func ok(_ sender: Any) {
        if !phone.isValidNumber {
            view.makeToast("Please enter your phone number with the country code:\n+123 12341234")
        } else {
            loading.isHidden = false
            let numberKit = PhoneNumberKit()
            number = numberKit.format(try! numberKit.parse(phone.text!), toType: .e164, withPrefix: true)
            let code = String(format: "%04d", Int(arc4random_uniform(10000)))
            EndpointsHandler().check(callback: { id in
                if id == nil {
                    EndpointsHandler().sms(callback: { notice in
                        if let notice = notice, notice.ok!.boolValue {
                            self.loading.isHidden = true
                            let pc = UIStoryboard(name: "Welcome", bundle: nil).instantiateViewController(withIdentifier: "phone2Controller") as! Phone2Controller
                            pc.epid = self.epid
                            pc._pw = self._pw
                            pc.number = self.number
                            pc._code = code
                            pc.phoneChange = self.phoneChange
                            self.present(pc, animated: true)
                            if self.phoneChange {
                                self.dismiss(animated: false)
                            }
                        } else {
                            self.view.makeToast("An error has occured :(")
                        }
                    }, number: self.number, body: "Your ePotato validation code: " + code)
                } else {
                    self.number = ""
                    self.loading.isHidden = true
                    self.view.makeToast("This phone number is already in use :(\nIf it is yours, try the 'Forgot Password' function.")
                }
            }, kind: EndpointsHandler.PROFILE, key: EndpointsHandler.P_PHONE, value: number)
        }
    }

    @IBAction func skip(_ sender: Any) {
        Phone2Controller.register(epid: epid, _pw: _pw, vc: self, loading: loading, phoneChange: phoneChange)
    }

    @IBAction func back(_ sender: Any) {
        if phoneChange {
            dismiss(animated: true)
        } else {
            let vc = UIStoryboard(name: "Welcome", bundle: nil).instantiateViewController(withIdentifier: "signupController") as! SignupController
            vc.epid = epid
            vc._pw = _pw
            present(vc, animated: true)
        }
    }
}
