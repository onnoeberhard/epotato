import UIKit

class PeoplePerson: UITableViewCell {

    var spc: SendPeopleController?
    var item: [String: String?]?
    var indexPath: IndexPath?

    @IBOutlet weak var box: UISwitch!
    @IBOutlet weak var label: UILabel!

    @IBAction func onchange(_ sender: Any) {
        spc?.selected[indexPath!] = box.isOn
    }

}
