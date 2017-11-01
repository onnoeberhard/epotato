import UIKit
import Toast_Swift

class Phone2Controller: UIViewController {

    @IBOutlet weak var notice: UILabel!
    @IBOutlet weak var code: UITextField!
    @IBOutlet weak var loading: UIActivityIndicatorView!

    var phoneChange = false

    var number = EndpointsHandler.NULL
    var _code = ""
    var epid = ""
    var _pw = ""

    override func viewDidLoad() {
        super.viewDidLoad()
        notice.text = "Please enter the code you just received (or will shortly receive) at:\n" + number
    }

    @IBAction func ok(_ sender: Any) {
        if code.text == _code {
            Phone2Controller.register(number: number, epid: epid, _pw: _pw, vc: self, loading: loading, phoneChange: phoneChange)
        } else {
            view.makeToast("The code is not correct :(")
        }
    }

    @IBAction func sendAgain(_ sender: Any) {
        loading.isHidden = false
        _code = String(format: "%04d", Int(arc4random_uniform(10000)))
        EndpointsHandler().sms(callback: { notice in
            self.loading.isHidden = true
            if notice == nil || !notice!.ok!.boolValue {
                self.view.makeToast("An error has occured :(")
            } else {
                self.view.makeToast("A new code has been sent to: " + self.number)
            }
        }, number: number, body: "Your ePotato validation code: " + _code)
    }

    static func register(number: String = EndpointsHandler.NULL, epid: String, _pw: String, vc: UIViewController, loading: UIActivityIndicatorView, phoneChange: Bool) {
        let pw = _pw.sha256()
        loading.isHidden = false
        let ud = UserDefaults.standard
        if phoneChange {
            EndpointsHandler().update(callback: { id in
                loading.isHidden = true
                if id != nil {
                    ud.set(number, forKey: LocalDatabaseHandler.PHONE)
                    vc.dismiss(animated: true)
                } else {
                    vc.view.makeToast("An error has occured! Sorry :(")
                }
            }, kind: EndpointsHandler.PROFILE, property: EndpointsHandler.ID,
                    value: String(ud.value(forKey: LocalDatabaseHandler.UID) as! Int),
                    properties: [EndpointsHandler.P_PHONE], values: [number])
        } else {
            EndpointsHandler().signup(callback: { id in
                loading.isHidden = true
                if let id = id, id.identifier != 0 {
                    ud.set(id.identifier, forKey: LocalDatabaseHandler.UID)
                    ud.set(epid, forKey: LocalDatabaseHandler.EPID)
                    ud.set(pw, forKey: LocalDatabaseHandler.PASSWORD)
                    ud.set(number == EndpointsHandler.NULL ? nil : number, forKey: LocalDatabaseHandler.PHONE)
                    ud.synchronize()
                    vc.present(UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "homeController"), animated: true)
                } else {
                    vc.view.makeToast("An error has occured! Sorry :(")
                }
            }, epid: epid, password: pw, phone: number)
        }
    }

    @IBAction func back(_ sender: Any) {
        let vc = UIStoryboard(name: "Welcome", bundle: nil).instantiateViewController(withIdentifier: "phone1Controller") as! Phone1Controller
        vc.phoneChange = phoneChange
        vc.number = number
        vc.epid = epid
        vc._pw = _pw
        present(vc, animated: true)
    }
}
