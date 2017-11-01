import UIKit

@IBDesignable class TopAlignedLabel: UILabel {
    override func drawText(in rect: CGRect) {
        if let stringText = text {
            let stringTextAsNSString = stringText as NSString
            let labelString = stringTextAsNSString.boundingRect(with: CGSize(width: frame.width, height: .greatestFiniteMagnitude),
                    options: .usesLineFragmentOrigin, attributes: [NSFontAttributeName: font], context: nil)
            super.drawText(in: CGRect(x: 0, y: 0, width: frame.width, height: ceil(labelString.size.height) > frame.height ? frame.height : ceil(labelString.size.height)))
        } else {
            super.drawText(in: rect)
        }
    }

    override func prepareForInterfaceBuilder() {
        super.prepareForInterfaceBuilder()
        layer.borderWidth = 1
        layer.borderColor = UIColor.black.cgColor
    }
}