import time
import urllib.request
import base64
import sys
import ssl

# Try to import win32print. If not installed, guide the user on how to install it.
try:
    import win32print
except ImportError:
    print("Error: 'pywin32' library is not installed.")
    print("Please install it by running: pip install pywin32")
    sys.exit(1)

# VPS Endpoint URL to poll for print jobs (HTTPS is strictly required on port 8080)
POLL_URL = "https://103.159.84.39:8080/PDI-TEST/print-label"
if len(sys.argv) > 1:
    POLL_URL = sys.argv[1]

# Create unverified SSL context to bypass self-signed certificate validation errors
ssl_context = ssl._create_unverified_context()

def run_pull_agent():
    print("=======================================================")
    print("  PDI Silent Printing Pull-Agent Active")
    print(f"  Polling URL: {POLL_URL}")
    try:
        current_printer = win32print.GetDefaultPrinter()
        print(f"  Target Default Printer: {current_printer}")
    except Exception:
        print("  WARNING: No default printer detected on this system!")
        print("  Please set a default printer in Windows Settings.")
    print("  Press Ctrl+C to stop.")
    print("=======================================================")

    while True:
        try:
            # Fetch a print job from the servlet queue (outbound GET request over HTTPS)
            req = urllib.request.Request(
                POLL_URL, 
                headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
            )
            with urllib.request.urlopen(req, context=ssl_context, timeout=5) as response:
                data = response.read().decode('utf-8').strip()
                
                # If we received a valid Base64 payload (not empty and not the 'NO_JOBS' token)
                if data and data != "NO_JOBS" and not data.startswith("ERROR:"):
                    print("[PULL-AGENT] Label received! Decoding and printing...")
                    raw_bytes = base64.b64decode(data)
                    
                    try:
                        printer_name = win32print.GetDefaultPrinter()
                    except Exception as e:
                        print(f"[PULL-AGENT] Error: Failed to retrieve default printer: {str(e)}")
                        time.sleep(1)
                        continue
                    
                    # Print raw bytes to the default printer
                    hPrinter = win32print.OpenPrinter(printer_name)
                    try:
                        hJob = win32print.StartDocPrinter(hPrinter, 1, ("PDI Label Print Job", None, "RAW"))
                        try:
                            win32print.StartPagePrinter(hPrinter)
                            win32print.WritePrinter(hPrinter, raw_bytes)
                            win32print.EndPagePrinter(hPrinter)
                            print(f"[PULL-AGENT] Successfully printed label to: {printer_name}")
                        finally:
                            win32print.EndDocPrinter(hPrinter)
                    except Exception as e:
                        print(f"[PULL-AGENT] Error writing to printer: {str(e)}")
                    finally:
                        win32print.ClosePrinter(hPrinter)
        except urllib.error.URLError as e:
            # Network issue or server offline, fail silently and retry
            pass
        except Exception as e:
            print(f"[PULL-AGENT] Unexpected error: {str(e)}")
            
        # Poll every 1 second
        time.sleep(1)

if __name__ == '__main__':
    try:
        run_pull_agent()
    except KeyboardInterrupt:
        print("\nStopping pull agent...")
