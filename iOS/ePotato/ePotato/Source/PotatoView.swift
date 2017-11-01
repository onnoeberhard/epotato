import UIKit

class PotatoView: UIView {

    let potatoForms = [Int](1...52)
    let forms: [Int: [String: Any]] = [
            1: ["img": "potato1", "point": CGPoint(x: 40, y: 35), "size": CGSize(width: 220, height: 135)],
            2: ["img": "potato2", "point": CGPoint(x: 55, y: 45), "size": CGSize(width: 225, height: 140)],
            3: ["img": "potato3", "point": CGPoint(x: 60, y: 30), "size": CGSize(width: 220, height: 135)],
            4: ["img": "potato4", "point": CGPoint(x: 30, y: 30), "size": CGSize(width: 230, height: 120)],
            5: ["img": "potato5", "point": CGPoint(x: 40, y: 20), "size": CGSize(width: 220, height: 125)],
            6: ["img": "potato6", "point": CGPoint(x: 45, y: 15), "size": CGSize(width: 220, height: 140)],
            7: ["img": "potato7", "point": CGPoint(x: 30, y: 60), "size": CGSize(width: 220, height: 140)],
            8: ["img": "potato8", "point": CGPoint(x: 65, y: 50), "size": CGSize(width: 225, height: 140)],
            9: ["img": "potato9", "point": CGPoint(x: 50, y: 40), "size": CGSize(width: 230, height: 150)],
            10: ["img": "potato10", "point": CGPoint(x: 40, y: 35), "size": CGSize(width: 230, height: 150)],
            11: ["img": "potato11", "point": CGPoint(x: 45, y: 20), "size": CGSize(width: 230, height: 130)],
            12: ["img": "potato12", "point": CGPoint(x: 50, y: 30), "size": CGSize(width: 220, height: 125)],
            13: ["img": "potato13", "point": CGPoint(x: 40, y: 15), "size": CGSize(width: 220, height: 130)],
            14: ["img": "potato14", "point": CGPoint(x: 40, y: 25), "size": CGSize(width: 220, height: 130)],
            15: ["img": "potato15", "point": CGPoint(x: 40, y: 30), "size": CGSize(width: 220, height: 125)],
            16: ["img": "potato16", "point": CGPoint(x: 60, y: 30), "size": CGSize(width: 230, height: 110)],
            17: ["img": "potato17", "point": CGPoint(x: 40, y: 30), "size": CGSize(width: 220, height: 140)],
            18: ["img": "potato18", "point": CGPoint(x: 50, y: 25), "size": CGSize(width: 240, height: 115)],
            19: ["img": "potato19", "point": CGPoint(x: 65, y: 30), "size": CGSize(width: 220, height: 125)],
            20: ["img": "potato20", "point": CGPoint(x: 30, y: 25), "size": CGSize(width: 215, height: 125)],
            21: ["img": "potato21", "point": CGPoint(x: 30, y: 40), "size": CGSize(width: 220, height: 150)],
            22: ["img": "potato22", "point": CGPoint(x: 30, y: 40), "size": CGSize(width: 220, height: 155)],
            23: ["img": "potato23", "point": CGPoint(x: 45, y: 40), "size": CGSize(width: 230, height: 155)],
            24: ["img": "potato24", "point": CGPoint(x: 40, y: 30), "size": CGSize(width: 240, height: 150)],
            25: ["img": "potato25", "point": CGPoint(x: 45, y: 30), "size": CGSize(width: 215, height: 155)],
            26: ["img": "potato26", "point": CGPoint(x: 60, y: 35), "size": CGSize(width: 210, height: 145)],
            27: ["img": "potato27", "point": CGPoint(x: 85, y: 30), "size": CGSize(width: 185, height: 150)],
            28: ["img": "potato28", "point": CGPoint(x: 40, y: 35), "size": CGSize(width: 210, height: 140)],
            29: ["img": "potato29", "point": CGPoint(x: 40, y: 30), "size": CGSize(width: 220, height: 135)],
            30: ["img": "potato30", "point": CGPoint(x: 65, y: 30), "size": CGSize(width: 215, height: 140)],
            31: ["img": "potato31", "point": CGPoint(x: 40, y: 25), "size": CGSize(width: 220, height: 140)],
            32: ["img": "potato32", "point": CGPoint(x: 45, y: 25), "size": CGSize(width: 240, height: 130)],
            33: ["img": "potato33", "point": CGPoint(x: 50, y: 30), "size": CGSize(width: 220, height: 140)],
            34: ["img": "potato34", "point": CGPoint(x: 60, y: 25), "size": CGSize(width: 220, height: 140)],
            35: ["img": "potato35", "point": CGPoint(x: 35, y: 30), "size": CGSize(width: 220, height: 140)],
            36: ["img": "potato36", "point": CGPoint(x: 60, y: 40), "size": CGSize(width: 220, height: 140)],
            37: ["img": "potato37", "point": CGPoint(x: 40, y: 30), "size": CGSize(width: 230, height: 150)],
            38: ["img": "potato38", "point": CGPoint(x: 45, y: 30), "size": CGSize(width: 230, height: 145)],
            39: ["img": "potato39", "point": CGPoint(x: 50, y: 30), "size": CGSize(width: 230, height: 120)],
            40: ["img": "potato40", "point": CGPoint(x: 40, y: 25), "size": CGSize(width: 230, height: 120)],
            41: ["img": "potato41", "point": CGPoint(x: 55, y: 35), "size": CGSize(width: 235, height: 140)],
            42: ["img": "potato42", "point": CGPoint(x: 50, y: 20), "size": CGSize(width: 225, height: 130)],
            43: ["img": "potato43", "point": CGPoint(x: 35, y: 30), "size": CGSize(width: 200, height: 140)],
            44: ["img": "potato44", "point": CGPoint(x: 65, y: 40), "size": CGSize(width: 220, height: 135)],
            45: ["img": "potato45", "point": CGPoint(x: 55, y: 45), "size": CGSize(width: 220, height: 160)],
            46: ["img": "potato46", "point": CGPoint(x: 40, y: 30), "size": CGSize(width: 230, height: 140)],
            47: ["img": "potato47", "point": CGPoint(x: 40, y: 30), "size": CGSize(width: 220, height: 125)],
            48: ["img": "potato48", "point": CGPoint(x: 60, y: 30), "size": CGSize(width: 220, height: 135)],
            49: ["img": "potato49", "point": CGPoint(x: 40, y: 20), "size": CGSize(width: 220, height: 125)],
            50: ["img": "potato50", "point": CGPoint(x: 50, y: 25), "size": CGSize(width: 230, height: 105)],
            51: ["img": "potato51", "point": CGPoint(x: 40, y: 30), "size": CGSize(width: 215, height: 135)],
            52: ["img": "potato52", "point": CGPoint(x: 60, y: 30), "size": CGSize(width: 220, height: 115)]
    ]

    var img: UIImage?
    var iv = UIImageView()

    let maxwidth: CGFloat = 320
    let maxfont: CGFloat = 80

    var _form: Int = 0
    var form: Int {
        set {
            if newValue == 0 {
                _form = potatoForms[potatoForms.startIndex.advanced(by: Int(arc4random_uniform(UInt32(potatoForms.count))))]
            } else {
                _form = newValue
            }
            self.img = UIImage(named: forms[form]!["img"] as! String)!
            let img = self.img!
            for subview in subviews {
                if subview.tag == 1 {
                    subview.removeFromSuperview()
                }
            }
            let container = UIView()
            container.tag = 1
            addSubview(container)
            container.translatesAutoresizingMaskIntoConstraints = false
            let width = bounds.width > maxwidth ? maxwidth : bounds.width
            container.widthAnchor.constraint(lessThanOrEqualToConstant: width).isActive = true
            container.heightAnchor.constraint(lessThanOrEqualToConstant: bounds.height).isActive = true
            container.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
            container.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
            iv = UIImageView(image: img)
            container.addSubview(iv)
            iv.translatesAutoresizingMaskIntoConstraints = false
            let ivWidth = width * img.size.height / img.size.width > bounds.height - 16 ? bounds.height * img.size.width / img.size.height - 16 : width - 16
            iv.widthAnchor.constraint(lessThanOrEqualToConstant: ivWidth).isActive = true
            iv.heightAnchor.constraint(lessThanOrEqualToConstant: ivWidth * img.size.height / img.size.width).isActive = true
            iv.centerXAnchor.constraint(equalTo: container.centerXAnchor).isActive = true
            iv.centerYAnchor.constraint(equalTo: container.centerYAnchor).isActive = true
            text = _text
        } get {
            return _form
        }
    }

    var _text: NSString = ""
    var text: NSString {
        set {
            if let img = img {
                _text = newValue
                UIGraphicsBeginImageContextWithOptions(img.size, false, UIScreen.main.scale)
                img.draw(in: CGRect(origin: CGPoint.zero, size: CGSize(width: 320, height: 320 * img.size.height / img.size.width)))
                var size: CGFloat = maxfont + 1
                var rect: CGRect
                let paragraphStyle = NSParagraphStyle.default.mutableCopy() as! NSMutableParagraphStyle
                paragraphStyle.alignment = NSTextAlignment.center
                repeat {
                    rect = text.boundingRect(with: CGSize(width: (forms[form]!["size"] as! CGSize).width, height: CGFloat.greatestFiniteMagnitude),
                            options: [NSStringDrawingOptions.usesLineFragmentOrigin],
                            attributes: [NSFontAttributeName: UIFont(name: "PatrickHand-Regular", size: size)!, NSParagraphStyleAttributeName: paragraphStyle],
                            context: nil)
                    size -= 1
                } while rect.height > (forms[form]!["size"] as! CGSize).height
                rect.origin = CGPoint(x: (forms[form]!["point"] as! CGPoint).x + (forms[form]!["size"] as! CGSize).width / 2 - rect.width / 2,
                        y: (forms[form]!["point"] as! CGPoint).y + (forms[form]!["size"] as! CGSize).height / 2 - rect.height / 2)
                text.draw(in: rect, withAttributes: [NSFontAttributeName: UIFont(name: "PatrickHand-Regular", size: size)!, NSParagraphStyleAttributeName: paragraphStyle])
//                let ctx = UIGraphicsGetCurrentContext()!
//                ctx.setStrokeColor(red: 0, green: 0, blue: 1, alpha: 1)
//                ctx.stroke(rect)
//                ctx.setStrokeColor(red: 0, green: 1, blue: 0, alpha: 1)
//                ctx.stroke(CGRect(origin: forms[form]!["point"] as! CGPoint, size: forms[form]!["size"] as! CGSize))
                let newImage = UIGraphicsGetImageFromCurrentImageContext()
                UIGraphicsEndImageContext()
                iv.image = newImage
            }
        } get {
            return _text
        }
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    func setup(form: Int = 0, text: NSString = "") {
        self.form = form
        self.text = text
    }

}
