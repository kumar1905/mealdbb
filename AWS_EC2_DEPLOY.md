# Deploy MealDB App to AWS EC2

This file contains step-by-step instructions to deploy the Spring Boot `mealdb-app` to an AWS EC2 instance. Follow each step on your machine/console; commands are PowerShell/local-machine (where noted) or Linux (on the EC2 instance).

## Prerequisites
- An AWS account with permissions to create EC2 instances and security groups.
- SSH key pair (create in AWS Console or use your own).
- Project built locally (JAR) or you can build on the EC2 instance. We recommend building locally and copying the JAR.

## Build the JAR locally
Run from your project root (Windows PowerShell):

```powershell
cd 'C:\Users\madas\OneDrive\Desktop\HCL'
mvn clean package -DskipTests
```

After success the JAR will be in `target/` (e.g. `target/mealdb-app-0.0.1-SNAPSHOT.jar`).

## Create an EC2 instance (console quick steps)
1. In AWS Console → EC2 → Launch Instance.
2. Choose AMI: **Ubuntu Server 22.04 LTS** (or Amazon Linux 2023).
3. Instance type: `t3.micro` (free tier eligible) or larger for production.
4. Key pair: select existing or create a new key pair (download .pem).
5. Configure security group: open ports below.
6. Launch instance and note the Public IPv4 address or Public DNS.

## Security Group rules (minimum)
- SSH: TCP 22 from your IP (restrict to your IP for security).
- HTTP: TCP 80 from 0.0.0.0/0 (if using Nginx reverse proxy).
- HTTPS: TCP 443 from 0.0.0.0/0 (if you will enable TLS).
- Application port (optional): TCP 8080 from 0.0.0.0/0 (if you run app directly without reverse proxy). For production prefer using a reverse proxy.

## SSH into the instance (from your machine)
On Windows (PowerShell + OpenSSH):

```powershell
# ensure pem has correct permissions only on WSL/GitBash/Linux; on Windows just use it
ssh -i C:\path\to\your-key.pem ubuntu@<EC2_PUBLIC_IP>
```

Replace `ubuntu` for Ubuntu AMI; use `ec2-user` for Amazon Linux.

## Install Java 21 on EC2 (Ubuntu example)
Once SSHed to EC2:

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y wget gnupg
# Install Temurin 21 (Eclipse Adoptium)
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmour -o /usr/share/keyrings/adoptium.gpg
echo 'deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb stable main' | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install -y temurin-21-jdk
java -version
```

## Copy your JAR to the EC2 instance
From your local machine (PowerShell with scp command available):

```powershell
# Replace paths and host
scp -i C:\path\to\your-key.pem target\mealdb-app-0.0.1-SNAPSHOT.jar ubuntu@<EC2_PUBLIC_IP>:/home/ubuntu/mealdb-app.jar
```

If `scp` isn't available on Windows, use WinSCP or SFTP client to upload the file.

## Create a systemd service to run the app (recommended)
On the EC2 instance create `/etc/systemd/system/mealdb.service` with the following content (use sudo):

```ini
[Unit]
Description=MealDB Spring Boot App
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu
ExecStart=/usr/bin/java -jar /home/ubuntu/mealdb-app.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Then enable & start the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable mealdb.service
sudo systemctl start mealdb.service
sudo journalctl -u mealdb.service -f
```

The app listens on port `8080` by default.

## (Optional) Configure Nginx as reverse proxy (recommended)
Install Nginx and configure to proxy requests from 80 → 8080.

```bash
sudo apt install -y nginx
sudo ufw allow 'Nginx Full'
sudo tee /etc/nginx/sites-available/mealdb <<'NGCONF'
server {
    listen 80;
    server_name _;

    location / {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
NGCONF

sudo ln -s /etc/nginx/sites-available/mealdb /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl restart nginx
```

Now access `http://<EC2_PUBLIC_IP>/` in your browser.

## Setup HTTPS (Let's Encrypt) — quick notes
- If you have a domain pointing to the EC2 public IP, install Certbot and run:

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d yourdomain.example.com
```

Follow prompts to obtain and auto-configure TLS.

## Alternative: Use Docker (if you prefer containers)
1. Build Docker image locally and push to ECR, or build on EC2.
2. Run container with `docker run -d --name mealdb -p 8080:8080 mealdb-image`.

## Automate via user-data (cloud-init) on instance launch
You can paste a `user-data` script when launching EC2 to install JDK, download the JAR from S3 (or your GitHub/URL), and start the service automatically.

Example minimal user-data (cloud-init):

```bash
#!/bin/bash
apt update
apt install -y wget
# install temurin 21 (commands from above)
# download jar from S3 or URL
wget https://example.com/mealdb-app.jar -O /home/ubuntu/mealdb-app.jar
# create systemd service and start
```

## Testing & Troubleshooting
- If `systemctl status mealdb` shows errors, check logs: `sudo journalctl -u mealdb.service -b`.
- Verify Java version: `java -version`.
- Confirm port open: `ss -tulpn | grep 8080`.
- If external requests fail, confirm Security Group and NACL allow the traffic and that Nginx (or app) is listening on the expected interface.

## Cleanup
- To stop and remove service:

```bash
sudo systemctl stop mealdb.service
sudo systemctl disable mealdb.service
sudo rm /etc/systemd/system/mealdb.service
sudo systemctl daemon-reload
```

## Summary (quick checklist)
- Build JAR locally: `mvn clean package`.
- Create EC2 instance (Ubuntu), open ports 22/80/443 (and 8080 if needed).
- SSH in, install Temurin 21, copy JAR, create systemd service, start it.
- Optionally configure Nginx and TLS.

If you want, I can also:
- Add a `mvnw` (Maven Wrapper) to the repo so you can run `./mvnw spring-boot:run` without installing Maven, or
- Provide an example `user-data` script that fully automates the install and run on instance launch.
