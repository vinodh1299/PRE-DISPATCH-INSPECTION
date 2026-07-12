using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Printing;
using System.IO;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Windows.Forms;

namespace PdiClient
{
    // =========================================================================
    // 1. ENTRY POINT & STYLES SYSTEM
    // =========================================================================
    static class Program
    {
        [STAThread]
        static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            
            // Set SecurityProtocol to support HTTPS and bypass certificate validation
            ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12 | SecurityProtocolType.Tls11 | SecurityProtocolType.Tls;
            ServicePointManager.ServerCertificateValidationCallback = delegate { return true; };

            LoginForm loginForm = new LoginForm();
            if (loginForm.ShowDialog() == DialogResult.OK)
            {
                Application.Run(new MainForm(loginForm.EmpName, loginForm.EmpNo, loginForm.LoginTime));
            }
        }
    }

    // =========================================================================
    // 2. LOGIN FORM
    // =========================================================================
    public class LoginForm : Form
    {
        private TextBox txtUsername;
        private TextBox txtPassword;
        private Button btnLogin;
        private Label lblError;

        public string EmpName { get; private set; }
        public string EmpNo { get; private set; }
        public string LoginTime { get; private set; }

        public LoginForm()
        {
            this.Text = "PDI - Operator Login";
            this.Size = new Size(380, 340);
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.StartPosition = FormStartPosition.CenterScreen;
            this.MaximizeBox = false;
            this.BackColor = Color.FromArgb(244, 246, 249);

            PictureBox pbLogo = new PictureBox() { Location = new Point(90, 15), Size = new Size(200, 50), SizeMode = PictureBoxSizeMode.StretchImage };
            try
            {
                using (WebClient client = new WebClient())
                {
                    byte[] bytes = client.DownloadData("https://103.159.84.39:8080/PDI-TEST/Assets/YUKEN LOGO.png");
                    using (MemoryStream ms = new MemoryStream(bytes))
                    {
                        pbLogo.Image = Image.FromStream(ms);
                    }
                }
            }
            catch { }

            Label lblTitle = new Label() { Text = "PRE DISPATCH INSPECTION", Location = new Point(20, 80), Size = new Size(340, 25), Font = new Font("Arial", 13, FontStyle.Bold), ForeColor = Color.FromArgb(0, 32, 96), TextAlign = ContentAlignment.MiddleCenter };
            Label lblUser = new Label() { Text = "Username / Employee No:", Location = new Point(30, 120), Size = new Size(320, 20), Font = new Font("Arial", 9, FontStyle.Bold) };
            txtUsername = new TextBox() { Location = new Point(30, 140), Size = new Size(300, 25), Font = new Font("Arial", 10) };
            Label lblPass = new Label() { Text = "Password:", Location = new Point(30, 170), Size = new Size(320, 20), Font = new Font("Arial", 9, FontStyle.Bold) };
            txtPassword = new TextBox() { Location = new Point(30, 190), Size = new Size(300, 25), Font = new Font("Arial", 10), UseSystemPasswordChar = true };

            btnLogin = new Button() { Text = "LOGIN", Location = new Point(30, 230), Size = new Size(300, 35), Font = new Font("Arial", 10, FontStyle.Bold), BackColor = Color.FromArgb(26, 159, 216), ForeColor = Color.White, FlatStyle = FlatStyle.Flat };
            btnLogin.FlatAppearance.BorderSize = 0;
            btnLogin.Click += BtnLogin_Click;

            lblError = new Label() { Location = new Point(30, 270), Size = new Size(300, 20), ForeColor = Color.Red, TextAlign = ContentAlignment.MiddleCenter, Font = new Font("Arial", 8, FontStyle.Italic) };

            this.Controls.AddRange(new Control[] { pbLogo, lblTitle, lblUser, txtUsername, lblPass, txtPassword, btnLogin, lblError });
            this.AcceptButton = btnLogin;
        }

        private void BtnLogin_Click(object sender, EventArgs e)
        {
            string username = txtUsername.Text.Trim();
            string password = txtPassword.Text;

            if (string.IsNullOrEmpty(username) || string.IsNullOrEmpty(password))
            {
                lblError.Text = "Please fill in all fields.";
                return;
            }

            btnLogin.Enabled = false;
            lblError.Text = "Authenticating...";
            Application.DoEvents();

            try
            {
                // Set up the POST request to LoginServlet
                var request = (HttpWebRequest)WebRequest.Create("https://103.159.84.39:8080/PDI-TEST/loginServlet");
                request.Method = "POST";
                request.ContentType = "application/x-www-form-urlencoded";
                request.AllowAutoRedirect = false; // Capture the redirect target

                string postData = string.Format("username={0}&password={1}", Uri.EscapeDataString(username), Uri.EscapeDataString(password));
                byte[] byteArray = Encoding.UTF8.GetBytes(postData);
                request.ContentLength = byteArray.Length;

                using (Stream dataStream = request.GetRequestStream())
                {
                    dataStream.Write(byteArray, 0, byteArray.Length);
                }

                using (var response = (HttpWebResponse)request.GetResponse())
                {
                    // Check if it's a redirect indicating successful login
                    if (response.StatusCode == HttpStatusCode.Redirect || response.StatusCode == HttpStatusCode.Moved)
                    {
                        string location = response.Headers["Location"];
                        if (location != null && location.Contains("index.html"))
                        {
                            // Extract emp_name, emp_no, and login_time from the query parameters
                            var uri = new Uri(new Uri("https://103.159.84.39:8080/"), location);
                            var query = System.Web.HttpUtility.ParseQueryString(uri.Query);

                            EmpName = query["emp_name"];
                            EmpNo = query["emp_no"];
                            LoginTime = query["login_time"];

                            this.DialogResult = DialogResult.OK;
                            this.Close();
                            return;
                        }
                    }

                    lblError.Text = "Invalid username or password.";
                }
            }
            catch (Exception ex)
            {
                lblError.Text = "Connection error: " + ex.Message;
            }
            finally
            {
                btnLogin.Enabled = true;
            }
        }
    }

    // =========================================================================
    // 3. MAIN PDI INSPECTION STATION FORM
    // =========================================================================
    public class MainForm : Form
    {
        private string empName;
        private string empNo;
        private string loginTime;
        private string currentShift = "General";

        private Image logoImage;
        private PictureBox pbHeaderLogo;
        private List<TextBox> inputs = new List<TextBox>();
        private List<ScannedItem> okList = new List<ScannedItem>();
        private HashSet<string> scannedValues = new HashSet<string>();

        private FlowLayoutPanel gridPanel;
        private Button btnAdd;
        private Button btnSave;
        private RadioButton rbGen, rbFirst, rbSecond, rbNight;

        public MainForm(string empName, string empNo, string loginTime)
        {
            this.empName = empName;
            this.empNo = empNo;
            this.loginTime = loginTime;

            this.Text = "PRE DISPATCH INSPECTION (PDI)";
            this.Size = new Size(1000, 700);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.BackColor = Color.White;

            InitializeComponents();
            DownloadLogo();
            GenerateInputFields(100);
        }

        private void InitializeComponents()
        {
            // --- HEADER ---
            Panel headerPanel = new Panel() { Dock = DockStyle.Top, Height = 100, BackColor = Color.FromArgb(0, 32, 96) };

            pbHeaderLogo = new PictureBox()
            {
                Location = new Point(20, 20),
                Size = new Size(180, 55),
                SizeMode = PictureBoxSizeMode.StretchImage,
                BackColor = Color.Transparent
            };

            Label lblTitle = new Label() { Text = "PRE DISPATCH INSPECTION (PDI)", Location = new Point(220, 20), Size = new Size(500, 30), Font = new Font("Arial", 16, FontStyle.Bold), ForeColor = Color.White };
            Label lblOperator = new Label() { Text = string.Format("OPERATOR NAME: {0}   |   EMP NO: {1}", empName, empNo), Location = new Point(220, 60), Size = new Size(600, 20), Font = new Font("Arial", 10, FontStyle.Bold), ForeColor = Color.FromArgb(26, 159, 216) };

            Button btnLogout = new Button() { Text = "LOGOUT", Location = new Point(880, 30), Size = new Size(80, 35), Font = new Font("Arial", 9, FontStyle.Bold), BackColor = Color.FromArgb(229, 57, 53), ForeColor = Color.White, FlatStyle = FlatStyle.Flat };
            btnLogout.FlatAppearance.BorderSize = 0;
            btnLogout.Click += (s, e) => { this.Close(); };

            headerPanel.Controls.AddRange(new Control[] { pbHeaderLogo, lblTitle, lblOperator, btnLogout });

            // --- BOTTOM CONTROLS ---
            Panel bottomPanel = new Panel() { Dock = DockStyle.Bottom, Height = 80, BackColor = Color.FromArgb(244, 246, 249) };

            // Shift Selector
            Label lblShift = new Label() { Text = "SHIFT:", Location = new Point(20, 30), Size = new Size(55, 20), Font = new Font("Arial", 10, FontStyle.Bold) };
            rbGen = new RadioButton() { Text = "General", Location = new Point(80, 28), Size = new Size(80, 24), Checked = true };
            rbFirst = new RadioButton() { Text = "First", Location = new Point(160, 28), Size = new Size(70, 24) };
            rbSecond = new RadioButton() { Text = "Second", Location = new Point(230, 28), Size = new Size(80, 24) };
            rbNight = new RadioButton() { Text = "Night", Location = new Point(310, 28), Size = new Size(70, 24) };

            rbGen.CheckedChanged += Shift_Changed;
            rbFirst.CheckedChanged += Shift_Changed;
            rbSecond.CheckedChanged += Shift_Changed;
            rbNight.CheckedChanged += Shift_Changed;

            btnAdd = new Button() { Text = "ADD FIELDS", Location = new Point(740, 20), Size = new Size(110, 40), Font = new Font("Arial", 9, FontStyle.Bold), BackColor = Color.White, ForeColor = Color.FromArgb(26, 159, 216), FlatStyle = FlatStyle.Flat };
            btnAdd.FlatAppearance.BorderColor = Color.FromArgb(26, 159, 216);
            btnAdd.FlatAppearance.BorderSize = 2;
            btnAdd.Click += (s, e) => GenerateInputFields(50);

            btnSave = new Button() { Text = "SAVE", Location = new Point(860, 20), Size = new Size(100, 40), Font = new Font("Arial", 10, FontStyle.Bold), BackColor = Color.FromArgb(76, 175, 80), ForeColor = Color.White, FlatStyle = FlatStyle.Flat };
            btnSave.FlatAppearance.BorderSize = 0;
            btnSave.Click += BtnSave_Click;

            bottomPanel.Controls.AddRange(new Control[] { lblShift, rbGen, rbFirst, rbSecond, rbNight, btnAdd, btnSave });

            // --- GRID INPUTS AREA ---
            gridPanel = new FlowLayoutPanel() { Dock = DockStyle.Fill, AutoScroll = true, Padding = new Padding(20) };

            this.Controls.AddRange(new Control[] { gridPanel, headerPanel, bottomPanel });
        }

        private void Shift_Changed(object sender, EventArgs e)
        {
            if (rbGen.Checked) currentShift = "General";
            else if (rbFirst.Checked) currentShift = "First";
            else if (rbSecond.Checked) currentShift = "Second";
            else if (rbNight.Checked) currentShift = "Night";
        }

        private void DownloadLogo()
        {
            try
            {
                using (WebClient client = new WebClient())
                {
                    byte[] bytes = client.DownloadData("https://103.159.84.39:8080/PDI-TEST/Assets/YUKEN LOGO.png");
                    using (MemoryStream ms = new MemoryStream(bytes))
                    {
                        logoImage = Image.FromStream(ms);
                        if (pbHeaderLogo != null)
                        {
                            pbHeaderLogo.Image = logoImage;
                        }
                    }
                }
            }
            catch
            {
                // Fallback if logo download fails (e.g. no server internet connection during launch)
                logoImage = null;
            }
        }

        private void GenerateInputFields(int count)
        {
            gridPanel.SuspendLayout();
            for (int i = 0; i < count; i++)
            {
                TextBox txt = new TextBox()
                {
                    Width = 175,
                    Height = 28,
                    Margin = new Padding(6),
                    Font = new Font("Arial", 10),
                    Tag = false // Tag represents if it was successfully scanned
                };
                txt.Leave += Txt_Leave;
                txt.KeyDown += (s, e) => { if (e.KeyCode == Keys.Enter) Txt_Leave(s, EventArgs.Empty); };

                inputs.Add(txt);
                gridPanel.Controls.Add(txt);
            }
            gridPanel.ResumeLayout();
        }

        private void Txt_Leave(object sender, EventArgs e)
        {
            TextBox txt = (TextBox)sender;
            string value = txt.Text.Trim();
            if (string.IsNullOrEmpty(value) || (bool)txt.Tag) return;

            // Check for duplicates
            if (scannedValues.Contains(value))
            {
                txt.BackColor = Color.Khaki;
                MessageBox.Show("Duplicate entry detected!", "Warning", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                txt.Focus();
                return;
            }

            scannedValues.Add(value);

            // Reformat serial number: remove hyphens, extract characters 6-10, prefix with 10000
            string cleanData = value.Replace("-", "");
            if (cleanData.Length < 10)
            {
                MessageBox.Show("Invalid barcode length. Must be at least 10 characters.", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                txt.Text = "";
                scannedValues.Remove(value);
                return;
            }
            string extracted = cleanData.Substring(5, 5);
            string qrData = "10000" + extracted;

            // Show Print Confirmation Dialog (just like the web page)
            using (PrintConfirmForm confirmForm = new PrintConfirmForm(value, qrData))
            {
                if (confirmForm.ShowDialog() == DialogResult.OK && confirmForm.Confirmed)
                {
                    txt.BackColor = Color.LightGreen;
                    txt.Tag = true; // Mark as successful

                    okList.Add(new ScannedItem() { scanned_data = value, label_status = "Label Print Done" });
                    PrintLabel(value);
                }
                else
                {
                    // Reset if cancelled
                    txt.Text = "";
                    txt.BackColor = Color.White;
                    scannedValues.Remove(value);
                }
            }
        }

        // =========================================================================
        // 4. NATIVE WINDOWS PRINTING API
        // =========================================================================
        private void PrintLabel(string serialNo)
        {
            PrintDocument pd = new PrintDocument();
            
            // Reformat serial number: remove hyphens, extract characters 6-10, prefix with 10000
            string cleanData = serialNo.Replace("-", "");
            string extracted = cleanData.Substring(5, 5);
            string qrData = "10000" + extracted;

            pd.PrintPage += (sender, ev) =>
            {
                // Dimensions in hundredths of an inch (1 inch = 2.54 cm = 25.4 mm)
                // Label size: 81.1mm width x 39.4mm height => 3.19 inches width x 1.55 inches height
                // Printable area in hundredths of an inch: ~319 x ~155
                
                Graphics g = ev.Graphics;
                g.PageUnit = GraphicsUnit.Display; // Uses hundredths of an inch

                // 1. Draw Yuken Logo
                if (logoImage != null)
                {
                    // Scale and place logo at top left: x=20 (~5mm), y=15 (~4mm)
                    g.DrawImage(logoImage, new RectangleF(20, 15, 98, 25));
                }
                else
                {
                    g.DrawString("YUKEN", new Font("Arial", 12, FontStyle.Bold), Brushes.Black, 20, 15);
                }

                // 2. Draw Serial Text (monospace): x=170, y=15
                g.DrawString(qrData, new Font("Courier New", 11, FontStyle.Bold), Brushes.Black, 170, 15);

                // 3. Draw Date Time: x=140, y=40
                string dateTimeStr = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss");
                g.DrawString(dateTimeStr, new Font("Arial", 8), Brushes.Black, 140, 40);

                // 4. Draw "PDI CHECK OK": x=20, y=65
                g.DrawString("PDI CHECK OK", new Font("Arial", 14, FontStyle.Bold), Brushes.Black, 20, 65);

                // 5. Draw Checkmark Icon (vector SVG/Line draw for maximum crispness)
                // Draws a custom checkmark path around x=30, y=100
                Pen checkPen = new Pen(Color.Black, 4);
                g.DrawLine(checkPen, 32, 110, 42, 120);
                g.DrawLine(checkPen, 42, 120, 58, 102);

                // 6. Draw QR Code
                // Downloads QR code image from public API on the fly
                try
                {
                    string qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + Uri.EscapeDataString(qrData);
                    using (WebClient webClient = new WebClient())
                    {
                        byte[] qrBytes = webClient.DownloadData(qrUrl);
                        using (MemoryStream ms = new MemoryStream(qrBytes))
                        {
                            Image qrImage = Image.FromStream(ms);
                            // Draw QR Code on the right side: x=177 (~45mm), y=67 (~17mm), size=70x70 (~18mm)
                            g.DrawImage(qrImage, new RectangleF(177, 67, 70, 70));
                        }
                    }
                }
                catch
                {
                    // Fallback square if network down for QR generation
                    g.DrawRectangle(Pens.Black, 177, 67, 70, 70);
                    g.DrawString("QR ERROR", new Font("Arial", 7), Brushes.Black, 185, 95);
                }
            };

            try
            {
                pd.Print();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Printing failed: " + ex.Message, "Printer Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        private void BtnSave_Click(object sender, EventArgs e)
        {
            if (okList.Count == 0)
            {
                MessageBox.Show("No successfully scanned items to save.", "Info", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            btnSave.Enabled = false;
            try
            {
                var request = (HttpWebRequest)WebRequest.Create("https://103.159.84.39:8080/PDI-TEST/save-results");
                request.Method = "POST";
                request.ContentType = "application/json";

                // Build scanned_items JSON array
                StringBuilder itemsJson = new StringBuilder();
                for (int i = 0; i < okList.Count; i++)
                {
                    itemsJson.AppendFormat(
                        "{{\"scanned_data\":\"{0}\",\"label_status\":\"{1}\"}}",
                        okList[i].scanned_data, okList[i].label_status
                    );
                    if (i < okList.Count - 1) itemsJson.Append(",");
                }

                string json = string.Format(
                    "{{\"emp_name\":\"{0}\",\"emp_no\":\"{1}\",\"shift\":\"{2}\",\"scanned_items\":[{3}]}}",
                    empName, empNo, currentShift, itemsJson.ToString()
                );

                byte[] byteArray = Encoding.UTF8.GetBytes(json);
                request.ContentLength = byteArray.Length;

                using (Stream dataStream = request.GetRequestStream())
                {
                    dataStream.Write(byteArray, 0, byteArray.Length);
                }

                using (var response = request.GetResponse())
                {
                    using (StreamReader reader = new StreamReader(response.GetResponseStream()))
                    {
                        string result = reader.ReadToEnd();
                        if (result.Contains("\"success\":true"))
                        {
                            MessageBox.Show("Inspection data successfully saved!", "Success", MessageBoxButtons.OK, MessageBoxIcon.Information);
                            
                            // Reset state
                            okList.Clear();
                            scannedValues.Clear();
                            foreach (var txt in inputs)
                            {
                                txt.Text = "";
                                txt.BackColor = Color.White;
                                txt.Tag = false;
                            }
                        }
                        else
                        {
                            MessageBox.Show("Save failed: " + result, "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Failed to save results: " + ex.Message, "Save Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally
            {
                btnSave.Enabled = true;
            }
        }
    }

    // =========================================================================
    // 5. PRINT CONFIRMATION DIALOG (MODAL POPUP)
    // =========================================================================
    public class PrintConfirmForm : Form
    {
        public bool Confirmed { get; private set; }

        public PrintConfirmForm(string serialNo, string qrData)
        {
            this.Text = "Print Label";
            this.Size = new Size(320, 320);
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.StartPosition = FormStartPosition.CenterParent;
            this.MaximizeBox = false;
            this.MinimizeBox = false;
            this.BackColor = Color.White;

            Label lblQuestion = new Label()
            {
                Text = "Print label for serial number?",
                Location = new Point(10, 15),
                Size = new Size(280, 20),
                Font = new Font("Arial", 10, FontStyle.Bold),
                TextAlign = ContentAlignment.MiddleCenter
            };

            PictureBox pbQr = new PictureBox()
            {
                Location = new Point(75, 45),
                Size = new Size(150, 150),
                SizeMode = PictureBoxSizeMode.StretchImage,
                BorderStyle = BorderStyle.FixedSingle
            };

            // Load QR Code dynamically
            try
            {
                string qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + Uri.EscapeDataString(qrData);
                using (WebClient client = new WebClient())
                {
                    byte[] bytes = client.DownloadData(qrUrl);
                    using (MemoryStream ms = new MemoryStream(bytes))
                    {
                        pbQr.Image = Image.FromStream(ms);
                    }
                }
            }
            catch
            {
                // Draw a fallback message if offline
                Bitmap bmp = new Bitmap(150, 150);
                using (Graphics g = Graphics.FromImage(bmp))
                {
                    g.Clear(Color.LightGray);
                    g.DrawString("QR Code\n(Offline)", new Font("Arial", 10), Brushes.Black, 45, 60);
                }
                pbQr.Image = bmp;
            }

            Button btnPrint = new Button()
            {
                Text = "PRINT LABEL",
                Location = new Point(60, 215),
                Size = new Size(180, 40),
                Font = new Font("Arial", 10, FontStyle.Bold),
                BackColor = Color.FromArgb(0, 70, 120),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btnPrint.FlatAppearance.BorderSize = 0;
            btnPrint.Click += (s, e) =>
            {
                this.Confirmed = true;
                this.DialogResult = DialogResult.OK;
                this.Close();
            };

            this.Controls.AddRange(new Control[] { lblQuestion, pbQr, btnPrint });
            this.AcceptButton = btnPrint;
        }
    }

    public class ScannedItem
    {
        public string scanned_data { get; set; }
        public string label_status { get; set; }
    }
}
