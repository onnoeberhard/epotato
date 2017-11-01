import UIKit

class ContactSingle: ContactCell {

    @IBOutlet weak var title: UILabel!

    func setup() {
        title.text = name_
    }

}
