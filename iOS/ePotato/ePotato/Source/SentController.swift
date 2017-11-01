import UIKit

class SentController: HomeController, UITableViewDataSource, UITableViewDelegate {

    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var sendButton: UIButton!

    let ldb = LocalDatabaseHandler()

    var items: [[String: String?]] = []
    var cells: [ListPotato] = []

    static var sc: SentController?

    override func viewDidLoad() {
        super.viewDidLoad()
        if #available(iOS 10.0, *) {
            tableView.refreshControl = refreshControl
        } else {
            tableView.backgroundView = refreshControl
        }
        SentController.sc = self
        tableView.delegate = self
        tableView.dataSource = self
        tableView.tableFooterView = UIView()
        refresh()
    }

    @IBAction func sendPotato(_ sender: Any) {
    }

    override func refresh() {
        refreshControl.beginRefreshing()
        update({
            self.refreshControl.endRefreshing()
        })
    }

    func update(_ onFinish: @escaping () -> ()) {
        onFinish()
        AppDelegate.updateFIEPID() {
            self.items = self.ldb.getAll(table: self.ldb.sentPotatoes)
            self.items.reverse()
            let group = DispatchGroup()
            group.enter()
            DispatchQueue.main.async {
                self.tableView.reloadData()
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
            let cell = tableView.dequeueReusableCell(withIdentifier: "listPotatoSmall") as! ListPotatoSmall
            cell.setup(items[indexPath.row], 2, self)
            cells.insert(cell, at: indexPath.row)
            return cell
        }
        return UITableViewCell()
    }

    public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return items.count
    }

    public func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return CGFloat(144)
    }

    public func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

}
