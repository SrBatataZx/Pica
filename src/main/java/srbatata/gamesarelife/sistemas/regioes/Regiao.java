package srbatata.gamesarelife.sistemas.regioes;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;

public class Regiao {

    private final String nome;
    private final String mundo;
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    // Armazena as flags da região (ex: "pvp" -> false, "construir" -> true)
    private final Map<String, Boolean> flags;

    public Regiao(String nome, String mundo, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.nome = nome.toLowerCase();
        this.mundo = mundo;
        // O Math.min e Math.max garantem que o minX será sempre o menor valor, independente da ordem da seleção
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.flags = new HashMap<>();
    }

    public boolean contem(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(mundo)) return false;
        return loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ;
    }

    public void setFlag(String flag, boolean valor) {
        flags.put(flag.toLowerCase(), valor);
    }

    public boolean getFlag(String flag, boolean valorPadrao) {
        return flags.getOrDefault(flag.toLowerCase(), valorPadrao);
    }

    public String getNome() { return nome; }
    public String getMundo() { return mundo; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
    public Map<String, Boolean> getFlags() { return flags; }
}