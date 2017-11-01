import UIKit

class ContactsTabController: UITabBarController {

    override func viewDidLoad() {
        super.viewDidLoad()
        tabBar.unselectedItemTintColor = UIColor(red: 0.00, green: 0.00, blue: 0.00, alpha: 0.4)
    }

    @IBAction func back(_ sender: Any) {
        navigationController?.dismiss(animated: true, completion: nil)
    }

    @IBAction func addperson(_ sender: Any) {
        switch tabBar.items!.index(of: tabBar.selectedItem!)! {
        case 0:
            ContactsController.cc?.addperson()
            break
        default:
            FollowingController.fc?.addperson()
            break
        }
    }

    @IBAction func reload(_ sender: Any) {
        switch tabBar.items!.index(of: tabBar.selectedItem!)! {
        case 0:
            ContactsController.cc?.reload()
            break
        default:
            FollowingController.fc?.reload()
            break
        }
    }
}
