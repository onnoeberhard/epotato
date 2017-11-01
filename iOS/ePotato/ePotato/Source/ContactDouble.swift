import UIKit

class ContactDouble: ContactCell {

    @IBOutlet weak var title: UILabel!
    @IBOutlet weak var subtitle: UILabel!

    func setup() {
        title.text = name_
        subtitle.text = name_ != epid ? epid : nil
    }

}
