import UIKit
import GoogleMobileAds

class AdController: UIViewController {

    @IBOutlet weak var adview: GADNativeExpressAdView!

    var request: GADRequest?

    override func viewDidLoad() {
        super.viewDidLoad()
        navigationItem.setHidesBackButton(true, animated: false)
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false
        adview.adUnitID = Credentials.GAD_UNIT_ID
        adview.rootViewController = self
        adview.load(request ?? GADRequest())
    }

    func setup() {
        request = GADRequest()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
    }

}
