import UIKit
import Toast_Swift

class ReceivedController: HomeController, UITableViewDataSource, UITableViewDelegate {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var sendButton: UIButton!

    let ldb = LocalDatabaseHandler()

    var items: [[String: String?]] = []
    var cells: [ListPotato] = []

    static var rc: ReceivedController?

    override func viewDidLoad() {
        super.viewDidLoad()
        if #available(iOS 10.0, *) {
            tableView.refreshControl = refreshControl
        } else {
            tableView.backgroundView = refreshControl
        }
        ReceivedController.rc = self
        tableView.delegate = self
        tableView.dataSource = self
        tableView.tableFooterView = UIView()
        refresh()
    }

    @IBAction func sendPotato(_ sender: Any) {
    }

    override func refresh() {
        self.ldb.truncate(self.ldb.newPotatoes)
        refreshControl.beginRefreshing()
        update({
            self.refreshControl.endRefreshing()
        })
    }

    public func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        if ldb.getAll(table: ldb.newPotatoes, column: ldb.newPotatoesPid).contains(where: { $0 == items[indexPath.row][LocalDatabaseHandler.ID]!! }) {
            return CGFloat(224)
        }
        return CGFloat(144)
    }

    func update(_ onFinish: @escaping () -> ()) {
        AppDelegate.updateFIEPID() {
            self.cells.removeAll()
            self.items = self.ldb.getAll(table: self.ldb.receivedPotatoes)
            self.items.reverse()
            let group = DispatchGroup()
            group.enter()
            DispatchQueue.main.async {
                self.tableView.reloadData()
                let n = self.ldb.getAll(table: self.ldb.newPotatoes).count
                self.tabBarItem.badgeValue = n > 0 ? String(n) : nil
                group.leave()
            }
            group.notify(queue: .main) {
                onFinish()
            }
        }
    }

    func notify() {
        sendButton.makeToast("New Potato!")
    }

    func notifyFeed() {
        sendButton.makeToast("New Potato in Feed!")
    }

    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        cells[indexPath.row].action()
        tableView.deselectRow(at: indexPath, animated: true)
    }

    public func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if items.count > indexPath.row {
            let cell = ldb.getAll(table: ldb.newPotatoes, column: ldb.newPotatoesPid).contains(where: { $0 == items[indexPath.row][LocalDatabaseHandler.ID]!! }) ?
                    tableView.dequeueReusableCell(withIdentifier: "listPotatoBig") as! ListPotatoBig :
                    tableView.dequeueReusableCell(withIdentifier: "listPotatoSmall") as! ListPotatoSmall
            cell.setup(items[indexPath.row], 0, self)
            cells.insert(cell, at: indexPath.row)
            return cell
        }
        return UITableViewCell()
    }

    public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return items.count
    }

    public func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
}
