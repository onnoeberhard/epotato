import UIKit

class WelcomeController: UIViewController {

    @IBOutlet weak var potatoView: PotatoView!

    override func viewDidLoad() {
        super.viewDidLoad()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        potatoView.setup(text: "Welcome 😁")
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }

}
