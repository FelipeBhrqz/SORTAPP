package data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import model.Resultado;
import model.VotoCandidato;
import TDAs.DoublyLinkedList;

public class ElectionDataLoader {
    private static final String RESOURCE = "/assets/presidentes_votacion_provincial_2025_formato_ancho.csv";

    // Carga todos los resultados del CSV (todas las vueltas)
    public static DoublyLinkedList<Resultado> loadAll() throws IOException {
        return loadAllByFilters(-1, -1); // -1 significa sin filtro
    }

    // Carga todos los resultados filtrando por vuelta (1 o 2)
    public static DoublyLinkedList<Resultado> loadAllByVuelta(int vuelta) throws IOException {
        return loadAllByFilters(-1, vuelta);
    }

    // Carga todos los resultados filtrando por año (por si el CSV contiene varios años)
    public static DoublyLinkedList<Resultado> loadAllByAnio(int anio) throws IOException {
        return loadAllByFilters(anio, -1);
    }

    // Núcleo de carga con filtros opcionales de año y vuelta
    private static DoublyLinkedList<Resultado> loadAllByFilters(int anioFiltro, int vueltaFiltro) throws IOException {
        InputStream is = ElectionDataLoader.class.getResourceAsStream(RESOURCE);
        if (is == null) throw new IOException("Recurso no encontrado: " + RESOURCE);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String headerLine = br.readLine();
            if (headerLine == null) throw new IOException("CSV vacío");
            String[] headers = headerLine.split(",");
            int anioIdx = indexOf(headers, "ANIO");
            int vueltaIdx = indexOf(headers, "VUELTA");
            int provinciaIdx = indexOf(headers, "PROVINCIA_NOMBRE");
            int sufragantesIdx = indexOf(headers, "SUFRAGANTES");
            if (anioIdx == -1 || vueltaIdx == -1 || provinciaIdx == -1 || sufragantesIdx == -1) {
                throw new IOException("No se encontraron las columnas requeridas en el encabezado");
            }
            DoublyLinkedList<Resultado> lista = new DoublyLinkedList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",");
                if (cols.length <= Math.max(provinciaIdx, sufragantesIdx)) continue; // línea corrupta
                String provincia = cols[provinciaIdx].trim();
                String sufrStr = cols[sufragantesIdx].trim();
                int sufragantes = parseEntero(sufrStr);
                int anio = parseEnteroSafe(cols, anioIdx);
                int vuelta = parseEnteroSafe(cols, vueltaIdx);
                // filtros opcionales
                if (anioFiltro != -1 && anio != anioFiltro) continue;
                if (vueltaFiltro != -1 && vuelta != vueltaFiltro) continue;
                lista.addLast(new Resultado(provincia, sufragantes, anio, vuelta));
            }
            return lista;
        }
    }

    // Retorna lista filtrada por provincia (más filtros opcionales)
    public static DoublyLinkedList<Resultado> loadByProvince(String provinciaFiltro) throws IOException {
        return loadByProvince(provinciaFiltro, -1); // compat: sin filtro vuelta
    }

    public static DoublyLinkedList<Resultado> loadByProvince(String provinciaFiltro, int vueltaFiltro) throws IOException {
        DoublyLinkedList<Resultado> all = loadAllByVuelta(vueltaFiltro == -1 ? -1 : vueltaFiltro);
        if (provinciaFiltro == null || provinciaFiltro.equalsIgnoreCase("Todas")) {
            return all;
        }
        DoublyLinkedList<Resultado> filtrada = new DoublyLinkedList<>();
        for (Resultado r : all) {
            if (r.getNombreProvincia().equalsIgnoreCase(provinciaFiltro)) {
                filtrada.addLast(r);
            }
        }
        return filtrada;
    }

    // Obtiene un listado de nombres de provincia (sin duplicados), se puede filtrar por vuelta si se desea
    public static DoublyLinkedList<String> getProvinceNames() throws IOException {
        return getProvinceNames(-1);
    }

    public static DoublyLinkedList<String> getProvinceNames(int vueltaFiltro) throws IOException {
        DoublyLinkedList<Resultado> all = (vueltaFiltro == -1) ? loadAll() : loadAllByVuelta(vueltaFiltro);
        DoublyLinkedList<String> provincias = new DoublyLinkedList<>();
        for (Resultado r : all) {
            boolean existe = false;
            for (String p : provincias) {
                if (p.equalsIgnoreCase(r.getNombreProvincia())) { existe = true; break; }
            }
            if (!existe) provincias.addLast(r.getNombreProvincia());
        }
        return provincias;
    }

    // Obtiene lista de nombres de candidatos (según encabezado, hasta antes de 'VOTOS VALIDOS')
    public static TDAs.DoublyLinkedList<String> getCandidateNames() throws IOException {
        InputStream is = ElectionDataLoader.class.getResourceAsStream(RESOURCE);
        if (is == null) throw new IOException("Recurso no encontrado: " + RESOURCE);
        TDAs.DoublyLinkedList<String> lista = new TDAs.DoublyLinkedList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String headerLine = br.readLine();
            if (headerLine == null) return lista;
            String[] headers = headerLine.split(",");
            int provinciaIdx = indexOf(headers, "PROVINCIA_NOMBRE");
            int start = provinciaIdx + 1; // después de PROVINCIA_NOMBRE
            for (int i = start; i < headers.length; i++) {
                String h = headers[i].trim();
                if (h.equalsIgnoreCase("VOTOS VALIDOS")) break; // fin de candidatos
                // excluir agregados si hubiera alguno inesperado
                if (h.length() == 0) continue;
                lista.addLast(h);
            }
        }
        return lista;
    }

    // Helper: determina si un candidato debe incluirse según la vuelta
    private static boolean includeCandidate(String nombreCandidato, int vuelta) {
        if (vuelta != 2) return true; // en primera vuelta incluir todos
        String n = nombreCandidato.trim().toUpperCase();
        return n.equals("LUISA GONZALEZ") || n.equals("DANIEL NOBOA AZIN");
    }

    // Carga votos de candidatos para una provincia y vuelta específica
    public static TDAs.DoublyLinkedList<VotoCandidato> loadCandidateVotesForProvince(String provincia, int vuelta) throws IOException {
        TDAs.DoublyLinkedList<VotoCandidato> votos = new TDAs.DoublyLinkedList<>();
        if (provincia == null || provincia.equalsIgnoreCase("Todas")) return votos; // requiere provincia específica
        InputStream is = ElectionDataLoader.class.getResourceAsStream(RESOURCE);
        if (is == null) throw new IOException("Recurso no encontrado: " + RESOURCE);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String headerLine = br.readLine();
            if (headerLine == null) return votos;
            String[] headers = headerLine.split(",");
            int provinciaIdx = indexOf(headers, "PROVINCIA_NOMBRE");
            int vueltaIdx = indexOf(headers, "VUELTA");
            int startCandidates = provinciaIdx + 1;
            int endCandidates = -1;
            for (int i = startCandidates; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase("VOTOS VALIDOS")) { endCandidates = i; break; }
            }
            if (endCandidates == -1) endCandidates = headers.length; // fallback si no está VOTOS VALIDOS
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",");
                if (cols.length < endCandidates) continue;
                String prov = cols[provinciaIdx].trim();
                if (!prov.equalsIgnoreCase(provincia)) continue;
                int vueltaVal = parseEnteroSafe(cols, vueltaIdx);
                if (vueltaVal != vuelta) continue;
                for (int c = startCandidates; c < endCandidates; c++) {
                    String nombreCandidato = headers[c].trim();
                    if (!includeCandidate(nombreCandidato, vuelta)) continue; // filtrar segunda vuelta
                    String valor = cols[c].trim();
                    int votosC = parseEntero(valor);
                    votos.addLast(new VotoCandidato(nombreCandidato, votosC));
                }
                break; // ya encontrada la provincia
            }
        }
        return votos;
    }

    // Agrega votos por candidato para todas las provincias en una vuelta dada
    public static DoublyLinkedList<VotoCandidato> loadCandidateVotesAllProvinces(int vuelta) throws IOException {
        DoublyLinkedList<VotoCandidato> resultado = new DoublyLinkedList<>();
        InputStream is = ElectionDataLoader.class.getResourceAsStream(RESOURCE);
        if (is == null) throw new IOException("Recurso no encontrado: " + RESOURCE);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String headerLine = br.readLine();
            if (headerLine == null) return resultado;
            String[] headers = headerLine.split(",");
            int vueltaIdx = indexOf(headers, "VUELTA");
            int provinciaIdx = indexOf(headers, "PROVINCIA_NOMBRE");
            int startCandidates = provinciaIdx + 1;
            int endCandidates = -1;
            for (int i = startCandidates; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase("VOTOS VALIDOS")) { endCandidates = i; break; }
            }
            if (endCandidates == -1) endCandidates = headers.length;
            // Inicializar candidatos según vuelta
            for (int c = startCandidates; c < endCandidates; c++) {
                String nombreCandidato = headers[c].trim();
                if (!includeCandidate(nombreCandidato, vuelta)) continue;
                resultado.addLast(new VotoCandidato(nombreCandidato, 0));
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",");
                int vueltaVal = parseEnteroSafe(cols, vueltaIdx);
                if (vueltaVal != vuelta) continue;
                // acumular
                int idxResultado = 0;
                for (int c = startCandidates; c < endCandidates; c++) {
                    String nombreCandidato = headers[c].trim();
                    if (!includeCandidate(nombreCandidato, vuelta)) continue;
                    String valor = cols[c].trim();
                    int votosC = parseEntero(valor);
                    // avanzar hasta idxResultado en lista resultado
                    int j = 0;
                    for (VotoCandidato vc : resultado) {
                        if (j == idxResultado) {
                            vc.setVotos(vc.getVotos() + votosC);
                            break;
                        }
                        j++;
                    }
                    idxResultado++;
                }
            }
        }
        return resultado;
    }

    // Helpers ------------------------------------------------
    private static int indexOf(String[] arr, String target) {
        for (int i = 0; i < arr.length; i++) {
            if (target.equalsIgnoreCase(arr[i].trim())) return i;
        }
        return -1;
    }

    private static int parseEntero(String valor) {
        try {
            if (valor.contains(".")) {
                double d = Double.parseDouble(valor);
                return (int)Math.round(d);
            }
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseEnteroSafe(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return 0;
        return parseEntero(cols[idx].trim());
    }
}