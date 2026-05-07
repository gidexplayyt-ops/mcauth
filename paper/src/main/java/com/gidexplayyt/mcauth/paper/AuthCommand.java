package com.gidexplayyt.mcauth.paper;

import com.gidexplayyt.mcauth.core.AccountData;
import com.gidexplayyt.mcauth.core.AuthManager;
import com.gidexplayyt.mcauth.core.BotType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public class AuthCommand implements CommandExecutor {
    private final AuthManager authManager;

    public AuthCommand(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команды доступны только игрокам.");
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        args = normalizeAlias(label.toLowerCase(), args);
        if (args.length == 0) {
            player.sendMessage("Использование: /mcauth register|login|link|confirm|status|license|admin");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "register" -> handleRegister(player, uuid, args);
            case "login" -> handleLogin(player, args);
            case "link" -> handleLink(player, uuid, args);
            case "confirm" -> handleConfirm(player, uuid, args);
            case "code" -> handleCode(player, uuid, args);
            case "status" -> handleStatus(player, uuid);
            case "license" -> handleLicense(player, uuid, args);
            case "admin" -> handleAdmin(player, args);
            default -> player.sendMessage("Неизвестная подкоманда. Используйте register|login|link|confirm|code|status|license|admin.");
        }
        return true;
    }

    private String[] normalizeAlias(String label, String[] args) {
        if ((label.equals("register") || label.equals("reg")) && args.length > 0) {
            String[] normalized = new String[args.length + 1];
            normalized[0] = "register";
            System.arraycopy(args, 0, normalized, 1, args.length);
            return normalized;
        }
        if ((label.equals("login") || label.equals("l")) && args.length > 0) {
            String[] normalized = new String[args.length + 1];
            normalized[0] = "login";
            System.arraycopy(args, 0, normalized, 1, args.length);
            return normalized;
        }
        if (label.equals("code") && args.length > 0) {
            String[] normalized = new String[args.length + 1];
            normalized[0] = "code";
            System.arraycopy(args, 0, normalized, 1, args.length);
            return normalized;
        }
        return args;
    }

    private void handleRegister(Player player, UUID uuid, String[] args) {
        if (args.length != 2) {
            player.sendMessage("Использование: /mcauth register <пароль>");
            return;
        }
        String password = args[1];
        AccountData account = authManager.register(uuid, player.getName(), password);
        if (account == null) {
            player.sendMessage("§cАккаунт с таким именем уже зарегистрирован.");
            return;
        }
        player.sendMessage("§aРегистрация успешна. Введите /mcauth login <пароль>.");
    }

    private void handleLogin(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage("Использование: /mcauth login <пароль>");
            return;
        }
        String password = args[1];
        Optional<AccountData> account = authManager.login(player.getName(), password);
        if (account.isEmpty()) {
            player.sendMessage("§cНеверный логин или пароль.");
            return;
        }
        player.sendMessage("§aВы успешно вошли.");
    }

    private void handleLink(Player player, UUID uuid, String[] args) {
        if (args.length != 3) {
            player.sendMessage("Использование: /mcauth link <telegram|vk|discord|google_authenticator> <идентификатор>");
            return;
        }
        BotType type = BotType.parse(args[1]);
        if (type == null) {
            player.sendMessage("§cНеверный тип бота. Доступно: telegram, vk, discord, google_authenticator.");
            return;
        }
        String identifier = args[2];
        if (authManager.linkBot(uuid, type, identifier)) {
            player.sendMessage("§aАккаунт привязан к " + type + ".");
            if (type == BotType.GOOGLE_AUTHENTICATOR) {
                player.sendMessage("§eЕсли вы привязали Google Authenticator, используйте код из приложения.");
            }
        } else {
            player.sendMessage("§cНе удалось привязать аккаунт. Сначала зарегистрируйтесь.");
        }
    }

    private void handleConfirm(Player player, UUID uuid, String[] args) {
        if (args.length != 2) {
            player.sendMessage("Использование: /mcauth confirm <код>");
            return;
        }
        String code = args[1];
        if (authManager.confirmLogin(uuid, code)) {
            player.sendMessage("§aВход подтвержден.");
        } else {
            player.sendMessage("§cКод неверный или срок действия истек.");
        }
    }

    private void handleStatus(Player player, UUID uuid) {
        int total = authManager.getAccountCount();
        int linked = authManager.getLinkedAccountCount(uuid);
        player.sendMessage("§6Всего зарегистрировано аккаунтов: §f" + total);
        player.sendMessage("§6Привязано сервисов: §f" + linked);
        player.sendMessage("§6Статус: §f" + (authManager.isAuthenticated(uuid) ? "в сети" : "ожидает авторизацию"));
    }

    private void handleCode(Player player, UUID uuid, String[] args) {
        if (args.length < 2) {
            player.sendMessage("Использование: /mcauth code <telegram|vk|discord> <идентификатор> или /mcauth code confirm <код>");
            return;
        }
        if (args[1].equalsIgnoreCase("confirm")) {
            if (args.length != 3) {
                player.sendMessage("Использование: /mcauth code confirm <код>");
                return;
            }
            String code = args[2];
            if (authManager.confirmBinding(uuid, code)) {
                player.sendMessage("§aПривязка к боту успешно выполнена.");
            } else {
                player.sendMessage("§cКод неверный или срок действия истек.");
            }
            return;
        }
        if (args.length != 3) {
            player.sendMessage("Использование: /mcauth code <telegram|vk|discord> <идентификатор>");
            return;
        }
        BotType type = BotType.parse(args[1]);
        if (type == null || type == BotType.GOOGLE_AUTHENTICATOR) {
            player.sendMessage("§cНеверный тип бота. Доступно: telegram, vk, discord.");
            return;
        }
        String identifier = args[2];
        String code = authManager.createBindingCode(uuid, type, identifier);
        if (code == null) {
            player.sendMessage("§cНе удалось создать код привязки. Убедитесь, что аккаунт зарегистрирован.");
            return;
        }
        player.sendMessage("§aКод привязки отправлен в выбранный сервис. Используйте /mcauth code confirm <код> после получения сообщения.");
    }

    private void handleLicense(Player player, UUID uuid, String[] args) {
        if (args.length != 2) {
            player.sendMessage("Использование: /mcauth license <ключ>");
            return;
        }
        String key = args[1];
        if (authManager.markLicensed(uuid, key)) {
            player.sendMessage("§aЛицензия зарегистрирована. Вы будете пропущены без пароля, если не привязали бот.");
        } else {
            player.sendMessage("§cНе удалось применить лицензию.");
        }
    }

    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("mcauth.admin")) {
            player.sendMessage("§cУ вас нет доступа к админ-командам.");
            return;
        }
        if (args.length == 1) {
            player.sendMessage("§eИспользование: /mcauth admin list|reload");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "list" -> player.sendMessage("§6Всего аккаунтов: §f" + authManager.getAccountCount());
            case "reload" -> player.sendMessage("§6Перезагрузка конфигурации не реализована в этой версии.");
            default -> player.sendMessage("§cНеизвестная админ-команда.");
        }
    }
}
