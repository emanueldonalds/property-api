## Initial installation
1. Run install.sh with as root
2. Enter environment variables in override.conf
3. Run `./gradlew bootJar`
4. Run `sudo systmectl daemon-reload` and sudo `sudo systemctl enable property-api`

## To deploy
1. Run `./gradlew bootJar`
2. Run `sudo systemctl restart property-api`

## Logs
`journalctl -u property-api`
