## Initial installation
1. Run install.sh as root
2. Enter environment variables in override.conf
3. Run 
```
./gradlew bootJar
sudo systmectl daemon-reload 
sudo systemctl enable property-api
```

## To deploy
```
./gradlew bootJar
sudo systemctl restart property-api
```

## Logs
`journalctl -u property-api`
