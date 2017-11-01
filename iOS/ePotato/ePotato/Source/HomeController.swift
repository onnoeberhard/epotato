import UIKit

class HomeController: UIViewController {

    let refreshControl = UIRefreshControl()

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        refreshControl.addTarget(self, action: #selector(refresh_), for: .valueChanged)
        SendPotatoController.form = 0
        SendPotatoController.text = ""
    }

    @objc func refresh_() {
        AppDelegate.loadContacts(self, {
            self.refresh()
        }, showWarning: false)
    }

    func refresh() {
    }
}